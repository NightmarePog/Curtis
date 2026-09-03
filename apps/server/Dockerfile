FROM gradle:9.3.1-jdk17 AS builder
WORKDIR /build
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
RUN addgroup -S curtis && adduser -S curtis -G curtis \
    && mkdir -p /data/import /data/media \
    && chown -R curtis:curtis /app /data

EXPOSE 3001
USER curtis
ENTRYPOINT ["java","-jar","app.jar"]
