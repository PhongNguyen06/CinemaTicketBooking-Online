FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 cinema
COPY --from=build /build/target/cinema-server-14.0.0-SNAPSHOT.jar /app/server.jar
USER cinema
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/server.jar"]
