FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY . .

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"

RUN ./gradlew --version

RUN ./gradlew clean bootJar -x test --no-daemon
