"""
S3 / RustFS image client for downloading observation images.

Uses boto3 to connect to the S3-compatible storage (RustFS/MinIO)
and download images by their S3 key.
"""

import io
import logging
from typing import Optional

import boto3
from botocore.exceptions import ClientError, NoCredentialsError
from PIL import Image

from worker.config import config

logger = logging.getLogger(__name__)


class S3Client:
    """Client for downloading images from S3 / RustFS storage."""

    def __init__(self) -> None:
        self.s3_client: Optional[boto3.client] = None
        self.bucket: str = config.S3_BUCKET

    def connect(self) -> None:
        """Initialize the S3 client with configured credentials."""
        try:
            self.s3_client = boto3.client(
                "s3",
                endpoint_url=config.S3_ENDPOINT,
                aws_access_key_id=config.S3_ACCESS_KEY,
                aws_secret_access_key=config.S3_SECRET_KEY,
                region_name=config.S3_REGION,
                config=boto3.session.Config(
                    s3={"addressing_style": "path" if config.S3_FORCE_PATH_STYLE else "virtual"},
                ),
            )
            # Verify connection
            self.s3_client.head_bucket(Bucket=self.bucket)
            logger.info("Connected to S3/RustFS at %s, bucket: %s", config.S3_ENDPOINT, self.bucket)
        except NoCredentialsError:
            logger.error("AWS credentials not found. Check S3_ACCESS_KEY and S3_SECRET_KEY.")
            raise
        except ClientError as e:
            logger.error("Failed to connect to S3/RustFS: %s", e)
            raise

    def download_image(self, s3_key: str) -> Image.Image:
        """
        Download an image from S3 and return it as a PIL Image.

        Args:
            s3_key: The S3 object key (e.g., "observations/user-id/uuid.jpg")

        Returns:
            PIL Image object.

        Raises:
            Exception: If the download fails or the object is not an image.
        """
        if self.s3_client is None:
            raise RuntimeError("S3 client not connected. Call connect() first.")

        logger.info("Downloading image from S3: %s", s3_key)

        try:
            response = self.s3_client.get_object(Bucket=self.bucket, Key=s3_key)
            image_data = response["Body"].read()
        except ClientError as e:
            error_code = e.response.get("Error", {}).get("Code", "Unknown")
            if error_code == "NoSuchKey":
                logger.error("Image not found in S3: %s", s3_key)
            elif error_code == "NoSuchBucket":
                logger.error("Bucket not found: %s", self.bucket)
            else:
                logger.error("S3 error downloading %s: %s", s3_key, e)
            raise

        try:
            image = Image.open(io.BytesIO(image_data))
            image.load()  # Force load to catch corrupted images
            image = image.convert("RGB")  # Ensure RGB format for model input
            logger.info("Downloaded image: %s (%dx%d)", s3_key, image.width, image.height)
            return image
        except Exception as e:
            logger.error("Failed to decode image from S3 key %s: %s", s3_key, e)
            raise


# Module-level singleton
_s3_client: Optional[S3Client] = None


def get_s3_client() -> S3Client:
    """Get or create the S3 client singleton."""
    global _s3_client
    if _s3_client is None:
        _s3_client = S3Client()
    return _s3_client


def connect_s3() -> S3Client:
    """Create, connect, and return the S3 client."""
    client = get_s3_client()
    client.connect()
    return client
