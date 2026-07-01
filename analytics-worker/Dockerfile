# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY transaction-gateway/pom.xml transaction-gateway/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY analytics-worker/pom.xml analytics-worker/pom.xml

COPY common common
COPY analytics-worker analytics-worker
RUN mvn -q -pl common,analytics-worker -am -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/analytics-worker/target/analytics-worker.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
