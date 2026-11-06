"""RabbitMQ client for the observation worker."""

import json
import logging
import time
from typing import Callable, Optional

import pika
import pika.exceptions
from worker.config import config

logger = logging.getLogger(__name__)


class RabbitMQClient:
    """RabbitMQ client for consuming observation jobs and publishing responses."""

    def __init__(self) -> None:
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.channel.Channel] = None
        self._connection_attempts: int = 0
        self._max_reconnect_delay: float = 60.0

    def _declare_queue_if_not_exists(self, queue_name: str, durable: bool = True, arguments: Optional[dict] = None) -> None:
        """Declare a queue if it doesn't already exist. Skips redeclaration to avoid PRECONDITION_FAILED."""
        try:
            self.channel.queue_declare(queue=queue_name, durable=durable, arguments=arguments)
            logger.info("Confirmed queue %s", queue_name)
        except pika.exceptions.ChannelClosedByBroker as e:
            if "PRECONDITION_FAILED" in str(e) or "inequivalent arg" in str(e):
                logger.info("Queue %s already exists with different arguments, using existing", queue_name)
                # PRECONDITION_FAILED closes the channel; reopen it
                self.channel = self.connection.channel()
            else:
                raise

    def connect(self) -> None:
        credentials = pika.PlainCredentials(username=config.RABBITMQ_USER, password=config.RABBITMQ_PASSWORD)
        params = pika.ConnectionParameters(host=config.RABBITMQ_HOST, port=config.RABBITMQ_PORT, credentials=credentials, heartbeat=600, blocked_connection_timeout=30, connection_attempts=3, retry_delay=5)
        try:
            self.connection = pika.BlockingConnection(params)
            self.channel = self.connection.channel()
            self._declare_queue_if_not_exists(
                queue_name=config.RABBITMQ_INPUT_QUEUE, durable=True,
                arguments={"x-message-ttl": 86400000, "x-dead-letter-exchange": config.RABBITMQ_EXCHANGE, "x-dead-letter-routing-key": f"{config.RABBITMQ_INPUT_QUEUE}.dlq"},
            )
            self._declare_queue_if_not_exists(
                queue_name=config.RABBITMQ_RESPONSE_QUEUE, durable=True,
                arguments={"x-message-ttl": 86400000, "x-dead-letter-exchange": config.RABBITMQ_EXCHANGE, "x-dead-letter-routing-key": f"{config.RABBITMQ_RESPONSE_QUEUE}.dlq"},
            )
            self.channel.exchange_declare(exchange=config.RABBITMQ_EXCHANGE, exchange_type='direct', durable=True)
            self.channel.queue_bind(queue=config.RABBITMQ_INPUT_QUEUE, exchange=config.RABBITMQ_EXCHANGE, routing_key=config.RABBITMQ_INPUT_QUEUE)
            logger.info("Bound queue %s to exchange %s", config.RABBITMQ_INPUT_QUEUE, config.RABBITMQ_EXCHANGE)
            self.channel.queue_bind(queue=config.RABBITMQ_RESPONSE_QUEUE, exchange=config.RABBITMQ_EXCHANGE, routing_key=config.RABBITMQ_RESPONSE_QUEUE)
            logger.info("Bound queue %s to exchange %s", config.RABBITMQ_RESPONSE_QUEUE, config.RABBITMQ_EXCHANGE)
            self._connection_attempts = 0
            logger.info("Connected to RabbitMQ at %s:%d", config.RABBITMQ_HOST, config.RABBITMQ_PORT)
        except pika.exceptions.AMQPConnectionError as e:
            logger.error("Failed to connect to RabbitMQ: %s", e)
            raise

    def reconnect(self) -> None:
        self._connection_attempts += 1
        delay = min(2 ** self._connection_attempts * 2, self._max_reconnect_delay)
        logger.warning("Reconnecting to RabbitMQ in %.1f seconds (attempt %d)...", delay, self._connection_attempts)
        time.sleep(delay)
        self.connect()

    def publish_response(self, response: dict) -> None:
        if self.channel is None or self.channel.is_closed:
            raise RuntimeError("RabbitMQ channel is not connected.")
        message = json.dumps(response)
        try:
            self.channel.basic_publish(exchange=config.RABBITMQ_EXCHANGE, routing_key=config.RABBITMQ_RESPONSE_QUEUE, body=message, properties=pika.BasicProperties(delivery_mode=2, content_type="application/json", correlation_id=response.get("observationId", "")))
            logger.info("Published response for observation %s", response.get("observationId", "unknown"))
        except pika.exceptions.AMQPChannelError as e:
            logger.error("AMQP channel error publishing response: %s", e)
            raise
        except pika.exceptions.AMQPConnectionError as e:
            logger.error("AMQP connection error publishing response: %s", e)
            raise

    def consume(self, callback: Callable, *args, consumer_tag: Optional[str] = None, **kwargs) -> None:
        if self.channel is None or self.channel.is_closed:
            raise RuntimeError("RabbitMQ channel is not connected.")
        self._connection_attempts = 0
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(
            queue=config.RABBITMQ_INPUT_QUEUE,
            on_message_callback=self._wrap_callback(callback, *args, **kwargs),
            consumer_tag=consumer_tag,
        )
        logger.info("Started consuming from queue %s (tag: %s)", config.RABBITMQ_INPUT_QUEUE, consumer_tag)
        self.channel.start_consuming()

    def _wrap_callback(self, callback: Callable, *args, **kwargs) -> Callable:
        def wrapper(ch, method, properties, body: bytes) -> None:
            observation_id = "unknown"
            try:
                message = json.loads(body)
                observation_id = message.get("observationId", "unknown")
                logger.info("Received job for observation %s (retry: %d/%d)", observation_id, message.get("retryCount", 0), message.get("maxRetries", 0))
                callback(ch, method, properties, body, *args, **kwargs)
            except json.JSONDecodeError as e:
                logger.error("Invalid JSON in message for observation %s: %s", observation_id, e)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            except Exception as e:
                logger.error("Error processing observation %s: %s", observation_id, e, exc_info=True)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        return wrapper

    def close(self) -> None:
        try:
            if self.channel and self.channel.is_open:
                self.channel.close()
        except Exception as e:
            logger.warning("Error closing channel: %s", e)
        try:
            if self.connection and self.connection.is_open:
                self.connection.close()
        except Exception as e:
            logger.warning("Error closing connection: %s", e)
        logger.info("RabbitMQ connection closed.")


_rabbitmq_client: Optional[RabbitMQClient] = None

def get_rabbitmq_client() -> RabbitMQClient:
    global _rabbitmq_client
    if _rabbitmq_client is None:
        _rabbitmq_client = RabbitMQClient()
    return _rabbitmq_client

def connect_rabbitmq() -> RabbitMQClient:
    client = get_rabbitmq_client()
    client.connect()
    return client
