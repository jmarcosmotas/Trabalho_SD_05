FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY dados ./dados

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/app.jar app.jar
COPY --from=build /app/dados ./dados

CMD ["java", "-jar", "app.jar"]