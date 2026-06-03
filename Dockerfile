# --- Stage 1: Build the application ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code and build the WAR package
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Production runtime environment ---
# Spring Boot 3.x relies on Jakarta EE 10, which requires Tomcat 10.1 or higher.
FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat


# Copy the generated WAR file from Stage 1 to Tomcat's webapps directory
# Renaming it to ROOT.war deploys your application at the root context path (/)
COPY --from=build /app/target/cms-0.0.1-SNAPSHOT.war webapps/ROOT.war

# Document the standard port Tomcat listens on
EXPOSE 8084

# Launch the Tomcat container
CMD ["catalina.sh", "run"]