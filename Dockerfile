# Stage 1: build React UI
FROM node:20-alpine AS ui-builder
WORKDIR /ui
COPY ui/package*.json ./
RUN npm ci --silent
COPY ui/ .
RUN npm run build

# Stage 2: build Spring Boot jar
FROM maven:3.9-eclipse-temurin-21-alpine AS app-builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src/ src/
COPY --from=ui-builder /ui/dist/ src/main/resources/static/
RUN mvn package -DskipTests -q

# Stage 3: runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=app-builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
