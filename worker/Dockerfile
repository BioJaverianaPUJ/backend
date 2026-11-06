# BioCLIP-2 Observation Worker

FROM python:3.11-slim-bookworm

WORKDIR /app

COPY worker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Optional: bake the model weights into the image (larger image, faster startup).
# Skip this if you mount a volume at HF_HOME instead.
# RUN python -c "import open_clip; open_clip.create_model_and_transforms('hf-hub:imageomics/bioclip-2')"

COPY worker/ ./worker/

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    HF_HOME=/models/hf \
    MODEL_NAME=imageomics/bioclip-2 \
    DEVICE=cpu \
    MODEL_PRECISION=float16 \
    RABBITMQ_HOST=rabbitmq \
    RABBITMQ_PORT=5672 \
    RABBITMQ_USER=rabbitmq \
    RABBITMQ_PASSWORD=rabbitmq \
    RABBITMQ_EXCHANGE=observation.jobs.exchange \
    RABBITMQ_INPUT_QUEUE=observation.jobs \
    RABBITMQ_RESPONSE_QUEUE=observation.jobs.response \
    RABBITMQ_RESPONSE_ROUTING_KEY=observation.jobs.response \
    S3_ENDPOINT=http://rustfs:9000 \
    S3_BUCKET=biojaveriana \
    S3_ACCESS_KEY=rustfsadmin \
    S3_SECRET_KEY=rustfsadmin \
    S3_REGION=us-east-1 \
    S3_FORCE_PATH_STYLE=true \
    INFERENCE_TEMPERATURE=100.0 \
    CONSUMER_COUNT=4 \
    LOG_LEVEL=INFO \
    SHUTDOWN_TIMEOUT=30.0

# Healthcheck verifies the worker can initialize (imports torch and open_clip).
HEALTHCHECK --interval=60s --timeout=15s --start-period=120s --retries=3 \
    CMD python -c "import torch, open_clip, sys; sys.exit(0)"

CMD ["python", "worker/main.py"]