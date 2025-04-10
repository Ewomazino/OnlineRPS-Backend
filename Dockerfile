# Build Stage: Use the Maven image with Eclipse Temurin JDK 17 (slim variant)
FROM maven:3.9.9-eclipse-temurin-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage: Use a Tomcat image with JDK 17 (slim variant)
FROM tomcat:9.0-jdk17-openjdk-slim
# Remove default Tomcat webapps for a clean container
RUN rm -rf /usr/local/tomcat/webapps/*
# Replace with your actual WAR file name
COPY --from=build /app/target/PropertyRentalSystem.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]