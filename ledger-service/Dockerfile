# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY transaction-gateway/pom.xml transaction-gateway/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY analytics-worker/pom.xml analytics-worker/pom.xml

COPY common common
COPY ledger-service ledger-service
RUN mvn -q -pl common,ledger-service -am -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ledger-service/target/ledger-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
