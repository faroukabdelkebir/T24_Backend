FROM openjdk:17
EXPOSE 8080
ADD target/extractor-process.jar extractor-process.jar
ENTRYPOINT ["java","-jar","/extractor-process.jar"]