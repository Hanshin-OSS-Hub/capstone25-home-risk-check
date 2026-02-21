# 1. 베이스 이미지
FROM openjdk:21-jdk-alpine

# 2. 작업 디렉토리
WORKDIR /app

# 3. 로컬에서 빌드된 JAR 복사
COPY build/libs/home-risk-check.jar app.jar

# 4. 포트 노출
EXPOSE 8080

# 5. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]