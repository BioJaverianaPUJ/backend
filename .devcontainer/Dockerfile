FROM mcr.microsoft.com/devcontainers/java:1-25-bookworm

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y postgresql-client \
    && rm -rf /var/lib/apt/lists/*