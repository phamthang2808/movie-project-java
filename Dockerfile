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
# SIMPLE & EFFECTIVE: Let Maven handle annotation processing correctly
# The key is ensuring Lombok is downloaded and annotation processing is enabled
ENV MAVEN_OPTS="-Xmx2048m"
# Verify Lombok is available in classpath
RUN mvn dependency:tree -Dincludes=org.projectlombok:lombok | head -20 || echo "Lombok check..."
# Clean and build in one go (ensures no delombok interference)
# The maven-compiler-plugin will process Lombok annotations correctly
RUN mvn clean package -DskipTests -U

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/movie-project-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Render sẽ set PORT env variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]

