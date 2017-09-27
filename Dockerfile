FROM openjdk:8
ADD . /redis-proxy
WORKDIR /redis-proxy
RUN ./gradlew jar
CMD java -jar ./build/libs/redis-proxy.jar localhost 5 5000
