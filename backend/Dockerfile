# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Etapa 2: imagen liviana para ejecutar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render define PORT; la app lo lee en server.port
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
