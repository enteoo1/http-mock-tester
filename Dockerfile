FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/mock-tester-1.0.0.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && mkdir -p /data
ENV APP_DB_PATH=/data/mocktester.db
VOLUME /data
EXPOSE 8443 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
