FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /projeto
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /projeto/target/gestao-eventos-1.0-SNAPSHOT.jar ./app.jar
COPY web ./web

EXPOSE 8080
CMD ["sh", "-c", "java -jar /app/app.jar --demo && exec java -jar /app/app.jar"]
