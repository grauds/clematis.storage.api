FROM eclipse-temurin:21-jdk

ENV ENV_NAME=local

ARG JAR_FILE=build/**/clematis.storage.api-*.jar
COPY ${JAR_FILE} app.jar

RUN mkdir -p /var/log/clematis
RUN mkdir -p /home/clematis/storage/files

RUN apt-get update && apt-get install -y curl

# there is a possibility to provide a set of environment variables to the docker container
# from the host environment using envfile, which will allow to customize internal container
# environment. It is recommended that each of variables in envfile is explicitly defined
# in the Dockerfile with a default value (enabling seamless local execution). JAVA_OPTS
# is given as an example here
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$ENV_NAME -jar app.jar"]
