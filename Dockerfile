FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG JAR_FILE=/workspace/target/*.jar
COPY --from=builder ${JAR_FILE} app.jar

RUN addgroup -S app && adduser -S app -G app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]