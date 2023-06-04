FROM eclipse-temurin:17.0.7_7-jre-jammy
RUN apt update
RUN apt install -y git ssh rsync
RUN chmod -R 777 /opt


