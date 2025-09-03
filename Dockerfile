# Use a slim JDK image for running Spring Boot
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the JAR built locally
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the jar - let Spring pick profiles via env var
ENTRYPOINT ["java", "-jar", "app.jar"]
