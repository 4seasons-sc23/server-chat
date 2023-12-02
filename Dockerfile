FROM docker.io/library/gradle:7-jdk19-focal AS builder
WORKDIR /build
COPY . /build
RUN gradle build -x test

FROM docker.io/library/openjdk:19-oracle

# redis
ENV REDIS_IP=redis_ip
ENV REDIS_PORT=redis_port
ENV REDIS_PASSWORD=redis_password
ENV TENANT_BASE_URL=tenant_server_url

EXPOSE 8080

RUN mkdir -p /usr/local/bin
COPY --from=builder /build/build/libs/*.jar /usr/local/bin/app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=${SERVER_MODE}", "-jar", "/usr/local/bin/app.jar"]
