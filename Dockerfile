# syntax=docker/dockerfile:1
# =============================================================================
# Parameterized multi-stage build for every Spring Boot backend service.
#
#   `SERVICE` selects the Maven module; `-am` also builds the shared-library
#   dependency. A BuildKit cache mount on ~/.m2 downloads deps only once.
#
# Used by (via the `SERVICE` build arg) : gateway-api, user-service,
#   workspace-service, chat-service, room-service, notifications-service,
#   calendar-service, tasks-service.
# =============================================================================

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
ARG SERVICE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${SERVICE} -am -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG SERVICE
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
