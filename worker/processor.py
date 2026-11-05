"""
Job processor for observation messages.

Orchestrates the full pipeline: consume message -> download image -> classify -> publish response.
"""

import json
import logging
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
        """
        observation_id = "unknown"
        try:
            # Deserialize message
            message = json.loads(body)
            observation_id = message.get("observationId", "unknown")
            s3_key = message.get("s3Key", "")

            if not s3_key:
                logger.error("Message missing s3Key for observation %s", observation_id)
                self._publish_error(ch, observation_id, "Missing s3Key in message")
                return

            logger.info("Processing observation %s: downloading image from S3 (%s)", observation_id, s3_key)

            # Download image from S3
            image = self.s3_client.download_image(s3_key)

            # Classify image
            logger.info("Classifying image for observation %s", observation_id)
            result = self.model_client.classify(image)

            # Build response
            response = {
                "observationId": observation_id,
                "success": True,
                "speciesCommonName": result.species,
                "certainty": result.certainty,
                "observationType": "ANIMAL" if result.is_animal else "PLANT",
                "errorMessage": None,
            }

            # Publish response
            self.rabbitmq_client.publish_response(response)

            # Acknowledge message
            ch.basic_ack(delivery_tag=method.delivery_tag)

            logger.info(
                "Completed observation %s: type=%s, species=%s, certainty=%.4f",
                observation_id,
                response["observationType"],
                response["speciesCommonName"],
                response["certainty"],
            )

        except Exception as e:
            logger.error(
                "Failed to process observation %s: %s",
                observation_id, e, exc_info=True,
            )
            self._publish_error(ch, observation_id, str(e))

    def _publish_error(
        self,
        ch: pika.channel.Channel,
        observation_id: str,
        error_message: str,
    ) -> None:
        """Publish an error response and acknowledge the message."""
        response = {
            "observationId": observation_id,
            "success": False,
            "speciesCommonName": None,
            "certainty": 0.0,
            "observationType": None,
            "errorMessage": error_message,
        }

        try:
            self.rabbitmq_client.publish_response(response)
            ch.basic_ack(delivery_tag=ch.last_delivery_tag)
            logger.info("Published error response for observation %s", observation_id)
        except Exception as e:
            logger.error("Failed to publish error response for observation %s: %s", observation_id, e)
