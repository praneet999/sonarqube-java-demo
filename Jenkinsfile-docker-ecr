FROM tomcat:10.1-jdk17-temurin

# Remove default sample applications, not Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy our Maven-generated WAR file into Tomcat
COPY target/sonarqube-java-demo.war /usr/local/tomcat/webapps/sonarqube-java-demo.war

# Tomcat listens on port 8080
EXPOSE 8080

# Start Tomcat in the foreground
CMD ["catalina.sh", "run"]
