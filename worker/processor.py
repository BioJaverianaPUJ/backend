"""
Job processor for observation messages.

Orchestrates the full pipeline: consume message -> download image -> classify -> publish response.
"""

import json
import logging
import time
from typing import Any, Optional

import pika

from worker.config import config
from worker.model_client import BioClipModelClient, get_model_client
from worker.rabbitmq_client import RabbitMQClient, get_rabbitmq_client
from worker.s3_client import S3Client, get_s3_client

logger = logging.getLogger(__name__)


class ObservationProcessor:
    """Processes observation jobs: downloads images, classifies them, publishes results."""

    def __init__(
        self,
        model_client: BioClipModelClient,
        s3_client: S3Client,
        rabbitmq_client: RabbitMQClient,
    ) -> None:
        self.model_client = model_client
        self.s3_client = s3_client
        self.rabbitmq_client = rabbitmq_client

    def process_message(
        self,
        ch: pika.channel.Channel,
        method: Any,
        properties: pika.BasicProperties,
        body: bytes,
    ) -> None:
        """
        Process a single observation job message.

        1. Deserialize the message
        2. Download the image from S3
        3. Run BioCLIP-2 classification
        4. Publish response to the response queue
        5. Acknowledge the message

        The message is always settled (acked, or nacked to the dead-letter
        queue as a last resort) on every exit path while the channel is
        still usable, so it can never be left "unacked" on a live channel —
        with prefetch=1 a single unacked message would permanently block
        this consumer.
        """
        observation_id = "unknown"
        started = time.monotonic()
        settled = False
        try:
            # Deserialize message
            message = json.loads(body)
            observation_id = message.get("observationId", "unknown")
            s3_key = message.get("s3Key", "")

            if not s3_key:
                logger.error("Message missing s3Key for observation %s", observation_id)
                settled = self._publish_error(ch, method, observation_id, "Missing s3Key in message")
                return

            logger.info("Processing observation %s: downloading image from S3 (%s)", observation_id, s3_key)

            # Download image from S3
            download_started = time.monotonic()
            image = self.s3_client.download_image(s3_key)
            logger.info("Downloaded image for observation %s in %.1fs", observation_id, time.monotonic() - download_started)

            # Classify image
            logger.info("Classifying image for observation %s", observation_id)
            classify_started = time.monotonic()
            result = self.model_client.classify(image)
            logger.info("Classified image for observation %s in %.1fs", observation_id, time.monotonic() - classify_started)

            # Build response
            response = {
                "observationId": observation_id,
                "success": True,
                "speciesCommonName": result.species,
                "certainty": result.certainty,
                "similarityScore": result.similarityScore,
                "observationType": {
                    "Animalia": "ANIMAL",
                    "Plantae": "PLANT",
                    "Fungi": "FUNGI",
                }.get(result.kingdom, "UNKNOWN"),
                "errorMessage": None,
            }

            # Publish response
            self.rabbitmq_client.publish_response(response)

            # Acknowledge message
            settled = self._ack(ch, method, observation_id)

            if settled:
                logger.info(
                    "Completed observation %s: type=%s, species=%s, certainty=%.4f (%.1fs total)",
                    observation_id,
                    response["observationType"],
                    response["speciesCommonName"],
                    response["certainty"],
                    time.monotonic() - started,
                )

        except Exception as e:
            logger.error(
                "Failed to process observation %s: %s",
                observation_id, e, exc_info=True,
            )
            settled = self._publish_error(ch, method, observation_id, str(e))

        # Last resort: never leave the message unacked on a live channel.
        # If the channel is already dead, the broker requeues the message
        # when the connection closes and the consumer reconnects.
        if not settled:
            self._nack(ch, method, observation_id)

    def _ack(self, ch: pika.channel.Channel, method: Any, observation_id: str) -> bool:
        """Acknowledge the message. Returns True if the message was settled."""
        try:
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return True
        except pika.exceptions.AMQPError as e:
            logger.error("Failed to acknowledge message for observation %s (connection likely lost): %s", observation_id, e)
            return False

    def _nack(self, ch: pika.channel.Channel, method: Any, observation_id: str) -> None:
        """Last-resort nack without requeue (routed to the dead-letter queue)."""
        try:
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            logger.warning(
                "Nacked message for observation %s without requeue (routed to dead-letter queue)",
                observation_id,
            )
        except pika.exceptions.AMQPError as e:
            logger.error(
                "Failed to nack message for observation %s (channel dead; broker will requeue when the connection closes): %s",
                observation_id, e,
            )

    def _publish_error(
        self,
        ch: pika.channel.Channel,
        method: Any,
        observation_id: str,
        error_message: str,
    ) -> bool:
        """Publish an error response and acknowledge the message.

        Returns True if the message was settled (error response published and
        acked). Returns False when that was not possible, in which case the
        caller falls back to nacking the message so it is never left unacked
        on a live channel.
        """
        response = {
            "observationId": observation_id,
            "success": False,
            "speciesCommonName": None,
            "certainty": 0.0,
            "similarityScore": None,
            "observationType": None,
            "errorMessage": error_message,
        }

        try:
            self.rabbitmq_client.publish_response(response)
        except Exception as e:
            logger.error("Failed to publish error response for observation %s: %s", observation_id, e)
            return False

        if self._ack(ch, method, observation_id):
            logger.info("Published error response for observation %s", observation_id)
            return True
        return False
