FROM maven:3.9-eclipse-temurin-22 AS builder
WORKDIR /app
# Copy the completely raw project
COPY pom.xml .
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=builder /app/target/VibeCodingMaster-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
