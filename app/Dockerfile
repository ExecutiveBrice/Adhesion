FROM alpine/java:22
# copy the packaged jar file into our docker image
COPY ./target/adhesion.jar /adhesion.jar

# The packaged application contains application.yml and its Spring profile
# variants. Do not override Spring's configuration search path here: doing so
# would prevent application-helloasso-sandbox.yml from being loaded.
CMD ["java", "-jar", "/adhesion.jar"]
