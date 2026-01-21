FROM maven:3.9-ibm-semeru-21-noble AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
COPY obi-java-agent.jar /app
ENTRYPOINT ["java", "-javaagent:obi-java-agent.jar", "-jar", "app.jar"]
