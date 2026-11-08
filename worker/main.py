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
import signal
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Optional

import pika

from worker.config import config
from worker.model_client import load_model
from worker.processor import ObservationProcessor
from worker.rabbitmq_client import connect_rabbitmq
from worker.s3_client import connect_s3

logger = logging.getLogger(__name__)

# Global reference for graceful shutdown
_rabbitmq_client: Optional[pika.BlockingConnection] = None


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
    """Handle shutdown signals."""
    logger.info("Received signal %d, shutting down...", signum)
    if _rabbitmq_client and _rabbitmq_client.connection and _rabbitmq_client.connection.is_open:
        _rabbitmq_client.close()
    sys.exit(0)


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

    # Initialize components
    logger.info("Connecting to S3/RustFS...")
    s3_client = connect_s3()

    logger.info("Loading BioCLIP-2 model...")
    model_client = load_model()

    logger.info("Connecting to RabbitMQ...")
    rabbitmq_client = connect_rabbitmq()

    # Create processor
    processor = ObservationProcessor(model_client, s3_client, rabbitmq_client)

    global _rabbitmq_client
    _rabbitmq_client = rabbitmq_client

    # Start consuming
    logger.info("Starting %d consumer(s)...", config.CONSUMER_COUNT)
    consumers = []

    try:
        for i in range(config.CONSUMER_COUNT):
            # Each consumer needs its own connection/channel to call start_consuming() concurrently
            consumer_client = connect_rabbitmq()
            consumer_processor = ObservationProcessor(model_client, s3_client, consumer_client)
            t = ThreadPoolExecutor(max_workers=1)
            future = t.submit(consumer_client.consume, consumer_processor.process_message, consumer_tag=f"worker-consumer-{i}")
            consumers.append((t, future, consumer_client))
            logger.info("Consumer %d started (thread pool future: %s)", i + 1, id(future))
    except KeyboardInterrupt:
        logger.info("Worker interrupted by user.")
    except Exception as e:
        logger.error("Worker failed to start: %s", e, exc_info=True)
        sys.exit(1)

    # Keep the main thread alive
    logger.info("Worker is running. Press Ctrl+C to stop.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down...")
    finally:
        # Close all consumer connections
        for executor, future, client in consumers:
            try:
                client.close()
            except Exception as e:
                logger.warning("Error closing consumer connection: %s", e)
        logger.info("Closing main RabbitMQ connection...")
        rabbitmq_client.close()
        logger.info("Worker stopped.")


if __name__ == "__main__":
    main()
