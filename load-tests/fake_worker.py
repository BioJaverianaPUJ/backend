"""Synthetic observation worker for isolated load tests; does not load BioCLIP."""

from __future__ import annotations

import ipaddress
import json
import logging
import math
import os
import time
from dataclasses import dataclass
from uuid import UUID

import pika

EXCHANGE = "observation.jobs.exchange"
INPUT_QUEUE = "observation.jobs"
RESPONSE_QUEUE = "observation.jobs.response"

logger = logging.getLogger("load-tests.fake-worker")


@dataclass
class Counters:
    consumed: int = 0
    responses_published: int = 0
    errors: int = 0


def _read_configuration() -> tuple[str, int, str, str, str, float]:
    host = os.environ.get("RABBITMQ_HOST", "127.0.0.1").strip()
    if host.lower() != "localhost":
        try:
            address = ipaddress.ip_address(host)
        except ValueError as error:
            raise ValueError("RABBITMQ_HOST must be localhost or a loopback IP.") from error
        if not address.is_loopback:
            raise ValueError("RABBITMQ_HOST must be localhost or a loopback IP.")

    port_text = (
        os.environ.get("RABBITMQ_PORT")
        or os.environ.get("LOADTEST_RABBIT_PORT")
        or "5673"
    )
    try:
        port = int(port_text)
    except ValueError as error:
        raise ValueError("RABBITMQ_PORT must be an integer.") from error
    if not 1 <= port <= 65535:
        raise ValueError("RABBITMQ_PORT must be between 1 and 65535.")

    username = os.environ.get("RABBITMQ_USER", "loadtest").strip()
    password = os.environ.get("RABBITMQ_PASSWORD", "")
    if not username or not password:
        raise ValueError("RABBITMQ_USER and RABBITMQ_PASSWORD must be set.")

    virtual_host = os.environ.get("RABBITMQ_VHOST", "/")
    try:
        delay = float(os.environ.get("FAKE_WORKER_DELAY_SECONDS", "0"))
    except ValueError as error:
        raise ValueError("FAKE_WORKER_DELAY_SECONDS must be a number.") from error
    if not math.isfinite(delay) or delay < 0:
        raise ValueError("FAKE_WORKER_DELAY_SECONDS must be a finite, non-negative number.")

    return host, port, username, password, virtual_host, delay


class FakeWorker:
    def __init__(self, channel: pika.adapters.blocking_connection.BlockingChannel, delay: float) -> None:
        self.channel = channel
        self.delay = delay
        self.counters = Counters()

    def _log_counters(self, event: str) -> None:
        logger.info(
            "%s: consumed=%d responses_published=%d errors=%d",
            event,
            self.counters.consumed,
            self.counters.responses_published,
            self.counters.errors,
        )

    def process_message(
        self,
        channel: pika.adapters.blocking_connection.BlockingChannel,
        method: pika.spec.Basic.Deliver,
        properties: pika.spec.BasicProperties,
        body: bytes,
    ) -> None:
        del properties
        self.counters.consumed += 1

        try:
            message = json.loads(body)
            if not isinstance(message, dict):
                raise ValueError("job must be a JSON object")
            observation_id = message.get("observationId")
            if not isinstance(observation_id, str) or not observation_id:
                raise ValueError("job must contain observationId")
            UUID(observation_id)
        except (UnicodeDecodeError, TypeError, ValueError) as error:
            self.counters.errors += 1
            logger.error(
                "Rejecting invalid job (%s); delivery_tag=%d",
                type(error).__name__,
                method.delivery_tag,
            )
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            self._log_counters("invalid job")
            return

        response = {
            "observationId": observation_id,
            "success": True,
            "speciesCommonName": "Load-test synthetic result",
            "certainty": 1.0,
            "similarityScore": 1.0,
            "observationType": "UNKNOWN",
            "errorMessage": None,
        }

        if self.delay:
            time.sleep(self.delay)

        try:
            published = channel.basic_publish(
                exchange=EXCHANGE,
                routing_key=RESPONSE_QUEUE,
                body=json.dumps(response, separators=(",", ":")).encode("utf-8"),
                properties=pika.BasicProperties(
                    delivery_mode=2,
                    content_type="application/json",
                    correlation_id=observation_id,
                ),
                mandatory=True,
            )
            if published is False:
                raise RuntimeError("RabbitMQ did not confirm the response publication.")
        except Exception as error:
            self.counters.errors += 1
            logger.error(
                "Could not publish response for observation %s (%s); input will remain unacked.",
                observation_id,
                type(error).__name__,
            )
            self._log_counters("publication failed")
            raise

        self.counters.responses_published += 1
        try:
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as error:
            self.counters.errors += 1
            logger.error(
                "Response for observation %s was published, but input ACK failed (%s).",
                observation_id,
                type(error).__name__,
            )
            self._log_counters("ACK failed")
            raise

        self._log_counters("processed")


def main() -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    connection: pika.BlockingConnection | None = None
    worker: FakeWorker | None = None
    exit_code = 0

    try:
        host, port, username, password, virtual_host, delay = _read_configuration()
        credentials = pika.PlainCredentials(username=username, password=password)
        parameters = pika.ConnectionParameters(
            host=host,
            port=port,
            virtual_host=virtual_host,
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=30,
            connection_attempts=3,
            retry_delay=5,
        )
        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

        channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
        channel.queue_declare(queue=INPUT_QUEUE, passive=True)
        channel.queue_declare(queue=RESPONSE_QUEUE, passive=True)
        channel.queue_bind(
            queue=RESPONSE_QUEUE,
            exchange=EXCHANGE,
            routing_key=RESPONSE_QUEUE,
        )
        channel.confirm_delivery()
        channel.basic_qos(prefetch_count=1)

        worker = FakeWorker(channel, delay)
        channel.basic_consume(queue=INPUT_QUEUE, on_message_callback=worker.process_message)
        logger.info(
            "Fake worker listening on isolated RabbitMQ %s:%d; input=%s response=%s delay=%.3fs",
            host,
            port,
            INPUT_QUEUE,
            RESPONSE_QUEUE,
            delay,
        )
        channel.start_consuming()
    except KeyboardInterrupt:
        logger.info("Ctrl+C received; stopping fake worker.")
    except Exception as error:
        logger.error("Fake worker stopped (%s).", type(error).__name__)
        exit_code = 1
    finally:
        if connection is not None and connection.is_open:
            try:
                connection.close()
            except (pika.exceptions.AMQPError, OSError) as error:
                logger.warning("RabbitMQ connection close failed (%s).", type(error).__name__)
        if worker is not None:
            worker._log_counters("stopped")

    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
