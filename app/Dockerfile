# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S adhesion \
    && adduser -S adhesion -G adhesion

WORKDIR /app
COPY --from=build --chown=adhesion:adhesion /workspace/target/adhesion.jar ./adhesion.jar

USER adhesion
EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=6 \
    CMD curl --fail --silent --show-error http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/adhesion.jar"]
