FROM gradle:8-jdk17 AS builder
WORKDIR /build
COPY . .
RUN gradle bootJar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 3001
ENTRYPOINT ["java","-jar","app.jar"]
