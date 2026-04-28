FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY . .

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
