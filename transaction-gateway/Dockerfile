# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the reactor poms first for better layer caching
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY transaction-gateway/pom.xml transaction-gateway/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY analytics-worker/pom.xml analytics-worker/pom.xml

# Copy sources and build only this service (+ its common dependency)
COPY common common
COPY transaction-gateway transaction-gateway
RUN mvn -q -pl common,transaction-gateway -am -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/transaction-gateway/target/transaction-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
