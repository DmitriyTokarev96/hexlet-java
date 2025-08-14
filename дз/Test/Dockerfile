# Используем Maven с OpenJDK 17 как базовый образ для сборки
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# Метаданные образа
LABEL maintainer="developer@example.com"
LABEL version="1.0.0"
LABEL description="Wallet Service - Spring Boot application"

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем pom.xml
COPY pom.xml .

# Скачиваем зависимости (этот слой будет кэширован, если pom.xml не изменился)
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src src

# Собираем приложение (пропускаем тесты для production образа)
RUN mvn clean package -DskipTests

# Второй этап - runtime образ
FROM openjdk:17-jdk-slim

# Метаданные образа
LABEL maintainer="developer@example.com"
LABEL version="1.0.0"
LABEL description="Wallet Service - Spring Boot application"

# Устанавливаем рабочую директорию
WORKDIR /app

# Создаем пользователя для безопасности (не root)
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser

# Копируем JAR файл и ресурсы из builder этапа
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/src/main/resources db/changelog

# Меняем владельца файлов на созданного пользователя
RUN chown -R appuser:appuser /app

# Переключаемся на непривилегированного пользователя
USER appuser

# Открываем порт
EXPOSE 8080

# Переменные окружения для JVM
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# Команда запуска
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
