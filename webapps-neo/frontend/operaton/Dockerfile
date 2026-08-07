FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B && \
    echo "=== Built JAR contents ===" && \
    jar tf target/*.jar | grep -E "spring-boot|operaton-engine" | head -10

FROM eclipse-temurin:21-jre-alpine

WORKDIR /operaton
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
