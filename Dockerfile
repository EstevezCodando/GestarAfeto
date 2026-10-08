# syntax=docker/dockerfile:1.7
# Servico principal (GestarAfeto). Contexto de build: raiz do repositorio.

# ---- Etapa 1: compilacao e extracao em camadas ------------------------------
FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
# O cache do Maven fica fora da imagem: rebuilds so baixam o que mudou.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
 && java -Djarmode=tools -jar target/GestarAfeto-*.jar extract --layers --destination /extracted

# ---- Etapa 2: runtime minimo, sem Maven, sem JDK, sem root ------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S -g 1001 spring && adduser -S -u 1001 -G spring spring
WORKDIR /app
# Camadas da menos para a mais volatil: dependencias raramente mudam, o codigo muda sempre.
COPY --from=build --chown=1001:1001 /extracted/dependencies/ ./
COPY --from=build --chown=1001:1001 /extracted/spring-boot-loader/ ./
COPY --from=build --chown=1001:1001 /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=1001:1001 /extracted/application/ ./

USER 1001:1001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "GestarAfeto-0.0.1-SNAPSHOT.jar"]
