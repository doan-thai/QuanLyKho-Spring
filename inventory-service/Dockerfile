# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy parent and all module POMs for Maven dependency and reactor resolution
COPY pom.xml ./
COPY common-lib/pom.xml ./common-lib/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY inventory-service/pom.xml ./inventory-service/
COPY supplier-service/pom.xml ./supplier-service/
COPY report-service/pom.xml ./report-service/
COPY ai-service/pom.xml ./ai-service/

# Copy common-lib and inventory-service source code
COPY common-lib/src ./common-lib/src
COPY inventory-service/src ./inventory-service/src

# Build common-lib and inventory-service
RUN mvn clean package -pl common-lib,inventory-service -am -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user and directory for uploaded images
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/uploads/images && \
    chown -R appuser:appgroup /app && \
    chown -R appuser:appgroup /app/uploads && \
    chmod -R 777 /app/uploads

VOLUME ["/app/uploads"]

# Copy built JAR from builder stage
COPY --from=builder /app/inventory-service/target/inventory-service-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
