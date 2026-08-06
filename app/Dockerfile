FROM alpine/java:21
# copy the packaged jar file into our docker image
COPY ./target/adhesion.jar /adhesion.jar
COPY ./src/main/resources/application.yml /resources/application.yml

# set the startup command to execute the jar
CMD ["java", "-jar", "/adhesion.jar", "--spring.config.location=/resources/application.yml"]

