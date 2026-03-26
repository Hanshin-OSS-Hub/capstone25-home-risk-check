# 1. 베이스 이미지
FROM node:24-alpine

# 2. 작업 디렉토리
WORKDIR /app

# 3. package.json과 package-lock.json 복사
COPY package*.json ./

# 4. 의존성 설치
RUN npm install

# 5. 소스 코드 복사
COPY . .

# 6. 포트 노출
EXPOSE 3000

# 7. 애플리케이션 실행
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# 배포를 위한 nginx 설정 필요