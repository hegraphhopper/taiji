FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.8.6-jdk-8 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=build /app/target/fund-platform-1.0.0.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
