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
FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat

# Clean up default Tomcat webapps to prevent conflicts
RUN rm -rf webapps/*

# Copy the generated WAR file from Stage 1 to Tomcat's webapps directory
COPY --from=build /app/target/cms-0.0.1-SNAPSHOT.war webapps/ROOT.war

# Document the standard port Tomcat listens on
# NOTE: Tomcat defaults to 8080 internally. If you want it to listen on 8084, 
# you must map it during 'docker run' or change Tomcat's server.xml config.
EXPOSE 8080

# Launch the Tomcat container
CMD ["catalina.sh", "run"]