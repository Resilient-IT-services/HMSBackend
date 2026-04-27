# Stage 1: Build (runs tests + coverage)
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy gradle wrapper & config first (for caching)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew

# Pre-download dependencies (cache layer)
RUN ./gradlew dependencies --no-daemon

# Copy full source
COPY . .

# FULL build (includes tests + jacoco coverage check)
RUN ./gradlew clean build --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy built jar
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
