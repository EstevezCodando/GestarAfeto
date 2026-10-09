# syntax=docker/dockerfile:1.7
# Microsservico de alertas. Contexto de build: alertas-service/.

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
 && java -Djarmode=tools -jar target/gestarafeto-alertas-*.jar extract --layers --destination /extracted

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S -g 1001 spring && adduser -S -u 1001 -G spring spring
WORKDIR /app
COPY --from=build --chown=1001:1001 /extracted/dependencies/ ./
COPY --from=build --chown=1001:1001 /extracted/spring-boot-loader/ ./
COPY --from=build --chown=1001:1001 /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=1001:1001 /extracted/application/ ./

USER 1001:1001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD ["wget", "-q", "--spider", "http://127.0.0.1:8081/actuator/health/readiness"]
ENTRYPOINT ["java", "-jar", "gestarafeto-alertas-0.0.1-SNAPSHOT.jar"]
