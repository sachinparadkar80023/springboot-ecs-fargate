FROM eclipse-temurin:17-jdk
EXPOSE 8080
ADD target/demo.jar demo.jar
ENTRYPOINT ["java","-jar","/demo.jar"]