# Multi-stage build for production
FROM openjdk:26-ea-21-slim as build

# Set working directory
WORKDIR /app

# Install curl for health checks in build stage
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests


# Production stage
#FROM openjdk:25-jre-slim
# Multi-stage build for production
FROM openjdk:26-ea-21-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

USER appuser


# Expose port 8080 (default Spring Boot port)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
CMD ["java", "-jar", "app.jar"]