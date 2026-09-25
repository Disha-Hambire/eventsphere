# ---- build stage: compile and package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first so they are cached between builds when only code changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- runtime stage: small JRE image, non-root user ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/eventsphere.jar app.jar
USER app

ENV PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://localhost:${PORT}/actuator/health" | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
