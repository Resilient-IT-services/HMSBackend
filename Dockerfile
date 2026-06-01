FROM gradle:8.8-jdk21 AS builder

WORKDIR /app

COPY . .

RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8070

ENTRYPOINT ["java","-jar","app.jar"]
