# we will use openjdk 8 with alpine as it is a very small linux distro
FROM openjdk:17-alpine
# copy the packaged jar file into our docker image
COPY ./target/asso.jar /asso.jar
COPY application.yml /resources/application.yml
COPY listingActivites.csv /listingActivites.csv
COPY import_adherents.csv /import_adherents.csv
# set the startup command to execute the jar
CMD ["java", "-jar", "/asso.jar", "--spring.config.location=/resources/application.yml"]


