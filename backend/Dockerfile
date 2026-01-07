# syntax=docker/dockerfile:1.4

FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .

# Baixa dependências usando cache de ~/.m2 para evitar download em todo build
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src src

# Build application (cache de ~/.m2 reaproveitado)
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
