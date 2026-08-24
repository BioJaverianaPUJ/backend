# --- Build stage ---
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Copy gradle/maven wrapper + dependency files first for better layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

# Now copy the rest and build
COPY src src
RUN ./gradlew bootJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN useradd --create-home --shell /bin/bash spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]