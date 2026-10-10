# Multi-stage Dockerfile for AI-Powered Fraud Detection System
# Stage 1: Build & Package
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src
COPY webapp ./webapp
COPY schema.sql ./schema.sql

# Build application and copy dependencies into target/lib
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV PORT=8080
EXPOSE 8080

# Copy application artifacts
COPY --from=build /app/target/fraud-detection-system-1.0.0.jar ./app.jar
COPY --from=build /app/target/lib ./lib
COPY --from=build /app/webapp ./webapp
COPY --from=build /app/src/webapp ./src/webapp
COPY --from=build /app/schema.sql ./schema.sql

# Launch application
CMD ["java", "-jar", "app.jar"]
