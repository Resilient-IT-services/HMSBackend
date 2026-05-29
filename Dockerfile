FROM gradle:8.8-jdk21 AS builder

WORKDIR /app

COPY . .

RUN gradle clean bootJar -x test --no-daemon
