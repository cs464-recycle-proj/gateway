# ===== Stage 1: Build =====
# Use Eclipse Temurin JDK 21 for building the application
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory for the build stage
WORKDIR /app

# Copy Maven configuration and wrapper files
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
COPY src ./src

# Make mvnw executable
RUN chmod +x mvnw

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# ===== Stage 2: Run =====
# Use Eclipse Temurin JRE 21 for running the application (smaller image)
FROM eclipse-temurin:21-jre-alpine

# Set working directory for the runtime stage
WORKDIR /app

# Create a non-root user for security
# Running as root in containers is a security risk
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership of the application files to the spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose the port the gateway listens on
EXPOSE 8080

# Add health check to monitor container health
# This helps orchestrators (Docker Compose, Kubernetes) detect if the service is healthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
