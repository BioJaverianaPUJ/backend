# --- Build stage ---
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN useradd --create-home --shell /bin/bash spring

# Copy only the executable boot jar (not *-plain.jar), with correct owner
COPY --from=build --chown=spring:spring /app/build/libs/*-SNAPSHOT.jar /app/app.jar
# If your version has no -SNAPSHOT suffix, set archiveFileName in Gradle instead (see below)

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]