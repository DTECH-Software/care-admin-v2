FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/care-admin.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/care-admin.jar"]
