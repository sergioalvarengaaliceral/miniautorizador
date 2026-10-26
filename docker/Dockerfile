FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src
COPY docker/Healthcheck.java /tmp/Healthcheck.java
RUN mkdir -p /tmp/healthcheck && javac -d /tmp/healthcheck /tmp/Healthcheck.java

RUN mvn -B package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/miniautorizador-0.0.1-SNAPSHOT.jar miniautorizador.jar
COPY --from=build /tmp/healthcheck /opt/healthcheck

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "miniautorizador.jar"]
