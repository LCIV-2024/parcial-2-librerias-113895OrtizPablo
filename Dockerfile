# // TODO: Implementar el Dockerfile

FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar.jar
ENTRYPOINT ["java", "-jar", "app.jar"]