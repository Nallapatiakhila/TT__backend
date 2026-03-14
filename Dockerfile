# Use a stable OpenJDK image
FROM eclipse-temurin:21-jdk-jammy

# Set working directory
WORKDIR /app

# Copy all source files
COPY . .

# Build the application
RUN ./mvnw -DskipTests package

# Expose the port that the app runs on (Spring Boot default is 8080, but can be overridden via PORT env)
EXPOSE 8090

# Use the PORT environment variable (Render sets this) and fall back to 8090
ENV PORT=8090

# Start the application
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
