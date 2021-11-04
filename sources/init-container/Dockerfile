#### CREACION DEL JAR ####
FROM adoptopenjdk/maven-openjdk11 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn -e -B dependency:go-offline
COPY src/ ./src
RUN cd /app && mvn -e -B package


#### EJECUCION DEL JAR ####
FROM openjdk:11

COPY --from=builder ./app/target/*.jar /app.jar

ENTRYPOINT exec java -jar /app.jar