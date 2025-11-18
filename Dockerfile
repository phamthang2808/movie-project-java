# Build stage - Linux AMD64 platform
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml và download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -U

# Copy source code
COPY src ./src

# Build với force update và verbose để debug
RUN mvn clean package -DskipTests -B -U --no-transfer-progress

# Runtime stage
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar file
COPY --from=build /app/target/movie-project-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# QUAN TRỌNG: Dùng PORT (không phải SERVER_PORT)
# Thêm memory limits cho free tier
ENTRYPOINT ["sh", "-c", "java -Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar"]