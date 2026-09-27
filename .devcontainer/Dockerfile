FROM eclipse-temurin:25-jdk-noble

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y postgresql-client git \
    && rm -rf /var/lib/apt/lists/*