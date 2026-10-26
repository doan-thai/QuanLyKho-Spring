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

# Copy common-lib and auth-service source code
COPY common-lib/src ./common-lib/src
COPY auth-service/src ./auth-service/src

# Build common-lib and auth-service
RUN mvn clean package -pl common-lib,auth-service -am -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /app/auth-service/target/auth-service-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
