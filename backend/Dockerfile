FROM maven:3.9.15-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
VOLUME /app/db
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
