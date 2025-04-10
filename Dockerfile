FROM gradle:7.6.1-jdk17 AS build
WORKDIR /app
COPY . /app/
RUN gradle build --no-daemon

FROM liberica-openjdk-alpine:18
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/gateway.jar

# Environment variables for service URLs
ENV AUTH_SERVICE_URL=http://auth-service:8081/auth
ENV LOGGING_SERVICE_URL=http://logging-service:8082/api/logging
ENV PROFILE_SERVICE_URL=http://profile-service:8083/api/profiles
ENV TRAINING_SERVICE_URL=http://training-service:8084/api/training
ENV FEED_SERVICE_URL=http://feed-service:8085/api/feed
ENV NOTES_SERVICE_URL=http://notes-service:8086/api/notebook
ENV DIET_SERVICE_URL=http://diet-service:8087/api/diet
ENV STATISTICS_SERVICE_URL=http://statistics-service:8088/api/statistics
ENV FILE_SERVICE_URL=http://file-service:8089/api/files

# Environment variables for Redis
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Environment variables for JWT
ENV JWT_SECRET=uXze_D0R-JcW9xqRE7p0ycq3pn56twM2tX0Krn0L9v1o5hc2d-cSP8JthSh9kP3G
ENV JWT_ISSUER=com.mad.gateway
ENV JWT_AUDIENCE=mad-mobile-app

# Create directory for logs
RUN mkdir -p /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/gateway.jar"]
