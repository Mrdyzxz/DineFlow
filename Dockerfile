
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY backend/src ./backend/src

RUN mkdir -p backend/bin && javac -d backend/bin backend/src/main/model/*.java backend/src/main/payment/*.java backend/src/main/service/*.java backend/src/main/app/*.java

EXPOSE 8080

CMD ["java", "-cp", "backend/bin", "app.ApiServer"]
