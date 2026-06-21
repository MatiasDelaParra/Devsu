FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG SERVICE
WORKDIR /workspace

COPY pom.xml .
COPY customer-service/pom.xml customer-service/pom.xml
COPY account-service/pom.xml account-service/pom.xml

RUN mvn -B -ntp dependency:go-offline -pl "${SERVICE}" -am

COPY customer-service/src customer-service/src
COPY account-service/src account-service/src

RUN mvn -B -ntp package -pl "${SERVICE}" -am -DskipTests \
    && cp "${SERVICE}"/target/*.jar /application.jar

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /application.jar application.jar

USER spring:spring
EXPOSE 8081 8082

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/application.jar"]
