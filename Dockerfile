# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
COPY src ./src

RUN ./mvnw clean package -DskipTests

# ===== Stage 2: Run =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR (regardless of exact name)
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
