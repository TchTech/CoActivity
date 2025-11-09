FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/CoActivity-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
