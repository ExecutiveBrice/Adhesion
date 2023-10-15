FROM openjdk:17-alpine
# copy the packaged jar file into our docker image
COPY ./asso.jar /asso.jar
COPY ./application.yml /resources/application.yml

# set the startup command to execute the jar
CMD ["java", "-jar", "/asso.jar", "--spring.config.location=/resources/application.yml"]


