##############################################
# 🏗️ 1️⃣ Build Stage
##############################################
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# GitHub Packages 인증용 ARG (빌드 시 --build-arg 로 전달됨)
ARG GITHUB_USER
ARG GITHUB_TOKEN

# 환경 변수로 전달 (Gradle이 System.getenv()로 참조 가능)
ENV GITHUB_USER=${GITHUB_USER}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# Gradle wrapper 및 소스 복사
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .

# Gradle 실행 권한 부여
RUN chmod +x gradlew

# Gradle 캐시 디렉토리 지정 (속도 개선)
ENV GRADLE_USER_HOME=/cache

# Gradle 빌드 수행 (GitHub Packages 접근 포함)
RUN ./gradlew clean bootJar --no-daemon

##############################################
# 🚀 2️⃣ Runtime Stage
##############################################
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Stage1에서 빌드된 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 실행할 명령
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
