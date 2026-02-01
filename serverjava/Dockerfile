FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /backend

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-jammy

WORKDIR /backend

COPY --from=build /backend/target/*.jar app.jar

EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]