# ---- Stage 1: build the jar with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---- Stage 2: run it with a small Java image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system swiftdrop && useradd --system --gid swiftdrop --no-create-home swiftdrop
COPY --from=build /app/target/*.jar app.jar
USER swiftdrop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]