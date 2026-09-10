# ---- Stage 1: builder ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw.cmd .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests


# ---- Stage 2: runtime ----
FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
