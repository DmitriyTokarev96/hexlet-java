# Wallet Service

Spring Boot приложение для управления кошельками пользователей с поддержкой высоких нагрузок и конкурентных операций.

## Описание

Wallet Service предоставляет REST API для выполнения операций с кошельками:
- Создание кошельков
- Пополнение баланса (депозит)
- Снятие средств (withdraw)
- Получение информации о балансе

## Технологический стек

- **Java 17** - язык программирования
- **Spring Boot 3.2.0** - основной фреймворк
- **Spring Data JPA** - для работы с базой данных
- **PostgreSQL 15** - реляционная база данных
- **Liquibase** - управление миграциями БД
- **Docker & Docker Compose** - контейнеризация
- **Spring Retry** - механизм повторных попыток
- **JUnit 5 & Mockito** - тестирование
- **TestContainers** - интеграционные тесты

## Архитектура

### Основные компоненты

1. **Wallet** - JPA сущность с поддержкой оптимистичной блокировки
2. **WalletService** - бизнес-логика с аннотацией @Retryable
3. **WalletController** - REST API контроллер
4. **WalletRepository** - Spring Data JPA репозиторий
5. **DTO классы** - для передачи данных между слоями

### Решение проблем конкурентности

- **Оптимистичная блокировка** через @Version в сущности Wallet
- **Автоматические повторные попытки** через @Retryable при конфликтах
- **Транзакционность** через @Transactional
- **Валидация данных** через Jakarta Validation

## API Endpoints

### 1. Создание кошелька
```
POST /api/v1/wallets
Query Parameters:
- initialBalance (optional): начальный баланс

Response: 201 Created
{
  "id": "uuid",
  "balance": "1000.00",
  "createdAt": "2024-01-01 12:00:00",
  "updatedAt": "2024-01-01 12:00:00"
}
```

### 2. Выполнение операции
```
POST /api/v1/wallet
Body:
{
  "walletId": "uuid",
  "operationType": "DEPOSIT" | "WITHDRAW",
  "amount": "100.00"
}

Response: 200 OK
{
  "success": true,
  "message": "Операция Депозит выполнена успешно",
  "walletId": "uuid",
  "operationType": "Депозит",
  "amount": "100.00",
  "newBalance": "1100.00",
  "timestamp": "2024-01-01 12:00:00"
}
```

### 3. Получение информации о кошельке
```
GET /api/v1/wallets/{walletId}

Response: 200 OK
{
  "id": "uuid",
  "balance": "1000.00",
  "createdAt": "2024-01-01 12:00:00",
  "updatedAt": "2024-01-01 12:00:00"
}
```

## Установка и запуск

### Предварительные требования

- Docker и Docker Compose
- Java 17+ (для локальной разработки)
- Maven 3.6+ (для локальной разработки)

### Быстрый запуск с Docker

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd Test
```

2. Запустите приложение:
```bash
docker-compose up -d
```

3. Проверьте статус:
```bash
docker-compose ps
```

4. Приложение доступно по адресу: http://localhost:8080

### Локальная разработка

1. Установите PostgreSQL и создайте базу данных
2. Настройте переменные окружения (см. .env.example)
3. Запустите приложение:
```bash
mvn spring-boot:run
```

## Конфигурация

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `DB_URL` | URL базы данных | `jdbc:postgresql://localhost:5432/wallet_db` |
| `DB_USERNAME` | Имя пользователя БД | `wallet_user` |
| `DB_PASSWORD` | Пароль БД | `wallet_password` |
| `SERVER_PORT` | Порт приложения | `8080` |
| `LOG_LEVEL` | Уровень логирования | `INFO` |

### Настройки базы данных

- **Пул соединений**: HikariCP с настраиваемыми параметрами
- **JPA**: Hibernate с оптимизациями для batch операций
- **Миграции**: Liquibase с автоматическим применением

## Тестирование

### Запуск тестов

```bash
# Все тесты
mvn test

# Только unit тесты
mvn test -Dtest=*Test

# Только интеграционные тесты
mvn test -Dtest=*IntegrationTest

# Тесты конкурентности
mvn test -Dtest=*ConcurrencyTest
```

### Типы тестов

1. **Unit тесты** - тестирование отдельных компонентов
2. **Интеграционные тесты** - тестирование взаимодействия компонентов
3. **Тесты конкурентности** - проверка работы при высоких нагрузках

## Производительность

### Обработка конкурентных запросов

- **1000 RPS** на один кошелек без 50x ошибок
- **Оптимистичная блокировка** предотвращает lost updates
- **Автоматические повторные попытки** при конфликтах версий
- **Настраиваемый пул соединений** для оптимальной производительности

### Мониторинг

- Health checks для всех сервисов
- Логирование операций с настраиваемым уровнем
- Метрики JPA/Hibernate для анализа производительности

## Безопасность

- **Валидация входных данных** через Jakarta Validation
- **Обработка исключений** с безопасными сообщениями об ошибках
- **Непривилегированный пользователь** в Docker контейнере
- **Проверка ограничений** на уровне базы данных

## Развертывание

### Docker Compose (разработка/тестирование)

```bash
docker-compose up -d
```

### Production

1. Соберите образ:
```bash
docker build -t wallet-service .
```

2. Запустите с production конфигурацией:
```bash
docker run -d \
  -e DB_URL=jdbc:postgresql://prod-db:5432/wallet_db \
  -e DB_USERNAME=prod_user \
  -e DB_PASSWORD=prod_password \
  -p 8080:8080 \
  wallet-service
```

## Структура проекта

```
Test/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── controller/     # REST контроллеры
│   │   │   ├── service/        # Бизнес-логика
│   │   │   ├── repository/     # Доступ к данным
│   │   │   ├── model/          # JPA сущности
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   └── exception/      # Пользовательские исключения
│   │   └── resources/
│   │       ├── db/changelog/   # Liquibase миграции
│   │       └── application.yml # Конфигурация
│   └── test/                   # Тесты
├── Dockerfile                  # Образ приложения
├── docker-compose.yml         # Оркестрация сервисов
├── init-db.sql               # Инициализация БД
└── pom.xml                   # Maven конфигурация
```

## Мониторинг и логирование

### Health Checks

- **Приложение**: http://localhost:8080/actuator/health
- **База данных**: автоматическая проверка через Docker healthcheck
- **PgAdmin**: http://localhost:5050 (если включен профиль admin)

### Логирование

- Настраиваемые уровни для разных компонентов
- Структурированные логи в JSON формате
- Ротация логов для production окружения

## Troubleshooting

### Частые проблемы

1. **Ошибка подключения к БД**: проверьте статус PostgreSQL контейнера
2. **Ошибки миграции**: проверьте права доступа пользователя БД
3. **Проблемы с конкурентностью**: увеличьте количество попыток в @Retryable

### Полезные команды

```bash
# Просмотр логов
docker-compose logs -f wallet-service

# Перезапуск сервиса
docker-compose restart wallet-service

# Проверка статуса БД
docker-compose exec postgres pg_isready -U wallet_user -d wallet_db
```

## Лицензия

MIT License

## Поддержка

Для вопросов и предложений создавайте issues в репозитории проекта.
