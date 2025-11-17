# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
COPY lombok.config .
# Download dependencies including Lombok explicitly
RUN mvn dependency:resolve -B && mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
# Force annotation processing by compiling first, then package
# Use -U to force update dependencies and ensure Lombok is downloaded
# Explicitly enable annotation processing
RUN mvn clean compile -DskipTests -U -Dmaven.compiler.proc=full && mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/movie-project-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Render sẽ set PORT env variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]

