# ===== 1️⃣ Build Stage =====
FROM eclipse-temurin:17-jdk-jammy AS stage1

WORKDIR /app

COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .

RUN chmod +x gradlew
RUN ./gradlew bootJar

# ===== 2️⃣ Runtime Stage =====
FROM eclipse-temurin:17-jre-jammy AS stage2

WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
