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

# Copy api-gateway source code
COPY api-gateway/src ./api-gateway/src

# Build api-gateway
RUN mvn clean package -pl api-gateway -am -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
