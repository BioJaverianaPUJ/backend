# BioCLIP-2 Observation Worker

FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY worker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY worker/ ./worker/

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    MODEL_NAME=imageomics/bioclip-2 \
    DEVICE=cuda \
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

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD python -c "import torch; print('CUDA available:', torch.cuda.is_available())" || exit 1

CMD ["python", "worker/main.py"]