"""
BioCLIP-2 Observation Worker - Entry Point

Consumes observation jobs from RabbitMQ, classifies images using BioCLIP-2,
and publishes results back to the response queue.

Usage:
    python worker/main.py

Configuration via environment variables (see worker/config.py for defaults):
    RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD
    S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY
    MODEL_NAME, DEVICE, MODEL_PRECISION
    ANIMAL_SPECIES_LIST (comma-separated), CONSUMER_COUNT
"""

import logging
import os
import signal
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor

import pika
import pika.exceptions

from worker.config import config
from worker.model_client import load_model
from worker.processor import ObservationProcessor
from worker.rabbitmq_client import RabbitMQClient, create_rabbitmq_client
from worker.s3_client import connect_s3

logger = logging.getLogger(__name__)

# Set on SIGINT/SIGTERM so consumer loops stop reconnecting and exit cleanly.
_shutdown = threading.Event()


def setup_logging() -> None:
    """Configure logging for the worker."""
    log_format = "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
    logging.basicConfig(
        level=getattr(logging, config.LOG_LEVEL.upper(), logging.INFO),
        format=log_format,
        stream=sys.stdout,
    )

    # Reduce noise from pika and urllib3
    logging.getLogger("pika").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("botocore").setLevel(logging.WARNING)


def signal_handler(signum, frame) -> None:
    """Handle shutdown signals.

    Raises SystemExit in the main thread; the finally block in main() sets
    _shutdown, closes the consumer connections and joins the consumer
    threads.
    """
    logger.info("Received signal %d, shutting down...", signum)
    sys.exit(0)


def _limit_inference_threads() -> None:
    """Limit PyTorch intra-op threads so concurrent inferences share the CPU.

    By default every inference grabs all cores; with CONSUMER_COUNT consumers
    running CPU inference concurrently that oversubscribes the box and makes
    each job (and its unacked window) much slower.
    """
    import torch

    cores = os.cpu_count() or 1
    threads = max(1, cores // max(1, config.CONSUMER_COUNT))
    torch.set_num_threads(threads)
    logger.info("PyTorch threads per inference: %d (cores=%d, consumers=%d)", threads, cores, config.CONSUMER_COUNT)


def _reconnect(client: RabbitMQClient, consumer_tag: str) -> None:
    """Reconnect with backoff until the connection is established again."""
    while not _shutdown.is_set():
        try:
            client.reconnect()
            logger.info("Consumer %s reconnected to RabbitMQ", consumer_tag)
            return
        except pika.exceptions.AMQPError as e:
            logger.error("Consumer %s reconnect failed: %s (retrying)", consumer_tag, e)
            time.sleep(5)


def _run_consumer(consumer_tag: str, client: RabbitMQClient, processor: ObservationProcessor) -> None:
    """Consume jobs, reconnecting automatically when the connection is lost.

    Runs in a dedicated thread (pika's BlockingConnection is not thread-safe:
    a connection must be created and driven by the thread that owns it).
    AMQP connection loss is handled here by reconnecting and resuming. The
    thread only exits during shutdown or on a non-recoverable error, which is
    re-raised so main() can notice the dead thread via its Future and restart
    the consumer with a fresh connection.
    """
    while not _shutdown.is_set():
        try:
            client.consume(processor.process_message, consumer_tag=consumer_tag)
            # start_consuming() returning (instead of raising) means the
            # connection was closed, e.g. by the broker; treat it as a loss.
            raise pika.exceptions.AMQPConnectionError("start_consuming() returned (connection closed)")
        except pika.exceptions.AMQPError as e:
            if _shutdown.is_set():
                logger.info("Consumer %s stopping (shutdown in progress)", consumer_tag)
                return
            logger.error("Consumer %s lost RabbitMQ connection: %s", consumer_tag, e)
            try:
                client.close()
            except Exception as close_error:
                logger.warning("Consumer %s error closing dead connection: %s", consumer_tag, close_error)
            _reconnect(client, consumer_tag)


def _start_consumer(index: int, model_client, s3_client) -> tuple:
    """Create a dedicated client/connection/thread for one consumer.

    Each consumer needs its own connection/channel: pika's BlockingConnection
    is not thread-safe, and start_consuming() must run in the thread that
    owns the connection.
    """
    consumer_tag = f"worker-consumer-{index}"
    consumer_client = create_rabbitmq_client()
    consumer_processor = ObservationProcessor(model_client, s3_client, consumer_client)
    executor = ThreadPoolExecutor(max_workers=1)
    future = executor.submit(_run_consumer, consumer_tag, consumer_client, consumer_processor)
    logger.info("Consumer %d started (tag: %s, thread pool future: %s)", index + 1, consumer_tag, id(future))
    return (executor, future, consumer_client, consumer_tag)


def _restart_dead_consumers(consumers: list, model_client, s3_client) -> None:
    """Restart consumer threads that died unexpectedly (non-AMQP errors).

    AMQP connection loss is already handled inside _run_consumer; anything
    that reaches here is an unexpected bug. Restarting the consumer with a
    fresh connection ensures one bad job cannot take the worker down.
    """
    for i, (executor, future, client, consumer_tag) in enumerate(consumers):
        if not future.done():
            continue
        exc = future.exception()
        if exc is not None:
            logger.error("Consumer %s thread died: %s", consumer_tag, exc, exc_info=exc)
        else:
            logger.warning("Consumer %s thread exited; restarting", consumer_tag)
        try:
            client.close()
        except Exception as e:
            logger.warning("Error closing dead consumer %s connection: %s", consumer_tag, e)
        executor.shutdown(wait=False)
        try:
            new_client = create_rabbitmq_client()
        except Exception as e:
            logger.error("Consumer %s restart deferred: could not connect (%s); retrying on next scan", consumer_tag, e)
            continue
        new_executor = ThreadPoolExecutor(max_workers=1)
        processor = ObservationProcessor(model_client, s3_client, new_client)
        new_future = new_executor.submit(_run_consumer, consumer_tag, new_client, processor)
        logger.warning("Consumer %s restarted (thread pool future: %s)", consumer_tag, id(new_future))
        consumers[i] = (new_executor, new_future, new_client, consumer_tag)



def main() -> None:
    """Main entry point for the observation worker."""
    setup_logging()
    logger.info("=" * 60)
    logger.info("BioCLIP-2 Observation Worker starting...")
    logger.info("Configuration:")
    logger.info("  Model: %s on %s (%s)", config.MODEL_NAME, config.DEVICE, config.MODEL_PRECISION)
    logger.info("  RabbitMQ: %s:%d", config.RABBITMQ_HOST, config.RABBITMQ_PORT)
    logger.info("  S3: %s (bucket: %s)", config.S3_ENDPOINT, config.S3_BUCKET)
    logger.info("  Kingdom labels: %s", config.KINGDOM_LABELS)
    logger.info("  Animal species: %d", len(config.ANIMAL_SPECIES_LIST))
    logger.info("  Consumer count: %d", config.CONSUMER_COUNT)
    logger.info("=" * 60)

    # Register signal handlers
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    # Split the CPU cores across consumers before any inference happens
    _limit_inference_threads()

    # Initialize components
    logger.info("Connecting to S3/RustFS...")
    s3_client = connect_s3()

    logger.info("Loading BioCLIP-2 model...")
    model_client = load_model()

    # Start consuming: each consumer gets its own connection, channel and
    # thread, driven by _run_consumer (which reconnects on connection loss).
    logger.info("Starting %d consumer(s)...", config.CONSUMER_COUNT)
    consumers: list = []

    try:
        for i in range(config.CONSUMER_COUNT):
            consumers.append(_start_consumer(i, model_client, s3_client))
    except KeyboardInterrupt:
        logger.info("Worker interrupted by user.")
    except Exception as e:
        logger.error("Worker failed to start: %s", e, exc_info=True)
        _shutdown.set()
        for executor, future, client, consumer_tag in consumers:
            try:
                client.close()
            except Exception:
                pass
            executor.shutdown(wait=False)
        sys.exit(1)

    # Keep the main thread alive and watch the consumer threads
    logger.info("Worker is running. Press Ctrl+C to stop.")
    try:
        while True:
            time.sleep(1)
            _restart_dead_consumers(consumers, model_client, s3_client)
    except KeyboardInterrupt:
        logger.info("Shutting down...")
    finally:
        _shutdown.set()
        # Close all consumer connections (makes the consume loops exit)
        for executor, future, client, consumer_tag in consumers:
            try:
                client.close()
                logger.info("Consumer %s connection closed", consumer_tag)
            except Exception as e:
                logger.warning("Error closing consumer %s connection: %s", consumer_tag, e)
        # With _shutdown set, the consumer threads exit their loops
        for executor, future, client, consumer_tag in consumers:
            try:
                executor.shutdown(wait=True)
            except Exception as e:
                logger.warning("Error shutting down consumer %s executor: %s", consumer_tag, e)
        logger.info("Worker stopped.")


if __name__ == "__main__":
    main()
