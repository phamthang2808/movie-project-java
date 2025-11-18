# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml và download dependencies trước
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build - quan trọng: phải clean trước
RUN mvn clean compile -B
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run với PORT từ environment variable
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
```

## **QUAN TRỌNG: Sửa lỗi tên file**

Bạn cần đổi tên file:
- `PaypalController.java` → `PayPalController.java`
- `PaypalConfig.java` → `PayPalConfig.java`

Hoặc đổi tên class trong file cho khớp với tên file hiện tại.

## **Kiểm tra lombok.config** (nếu có):

Nếu bạn có file `lombok.config` trong root project, hãy đảm bảo nó không block annotation processing:
```
# lombok.config
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true