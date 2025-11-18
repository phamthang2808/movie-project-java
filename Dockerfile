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
# BREAKTHROUGH APPROACH: Use delombok to generate code BEFORE compile
# This ensures getter/setter are generated as actual code before compilation
ENV MAVEN_OPTS="-Xmx2048m"
# Step 1: Generate sources using delombok (runs in generate-sources phase)
# This creates actual getter/setter code in target/delombok (with all Lombok annotations expanded)
RUN mvn generate-sources -DskipTests
# Step 2: Replace src/main/java with delombok output (now has actual getter/setter code, no Lombok annotations)
# Keep resources directory intact
RUN rm -rf src/main/java && mkdir -p src/main/java && cp -r target/delombok/* src/main/java/ && \
    find src/main/java -name "*.java" -type f | head -5 || echo "Delombok files copied"
# Step 3: Clean and compile (now compiling actual Java code with getter/setter, no annotation processing needed)
RUN mvn clean compile -DskipTests -U
# Step 4: Package
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/movie-project-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Render sẽ set PORT env variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]

