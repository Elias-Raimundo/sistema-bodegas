FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

COPY . .

RUN gradle build -x test

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java","-jar","app.jar"]