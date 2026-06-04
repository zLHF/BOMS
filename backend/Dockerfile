# ---- 多阶段构建：后端 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests -q 2>/dev/null || mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/boms-web/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
