FROM eclipse-temurin:21-jre
WORKDIR /build
COPY target/CoActivity-1.0-SNAPSHOT-shaded.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
