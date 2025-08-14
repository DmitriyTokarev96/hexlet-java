# Инструкции по развертыванию Wallet Service

## Быстрый старт

### 1. Запуск с Docker Compose

```bash
# Клонирование и переход в директорию
git clone <repository-url>
cd Test

# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f wallet-service
```

### 2. Проверка работоспособности

```bash
# Проверка API
curl http://localhost:8080/api/v1/wallets

# Создание кошелька
curl -X POST "http://localhost:8080/api/v1/wallets?initialBalance=1000.00"

# Проверка баланса (замените {WALLET_ID} на реальный ID)
curl http://localhost:8080/api/v1/wallets/{WALLET_ID}
```

## Конфигурация

### Переменные окружения

Создайте файл `.env` на основе `.env.example`:

```bash
# Скопируйте пример
cp .env.example .env

# Отредактируйте значения
nano .env
```

### Основные настройки

| Переменная | Описание | Рекомендуемое значение |
|------------|----------|------------------------|
| `DB_PASSWORD` | Пароль БД | Сложный пароль для production |
| `DB_MAX_POOL_SIZE` | Размер пула соединений | 20-50 в зависимости от нагрузки |
| `LOG_LEVEL` | Уровень логирования | INFO для production, DEBUG для разработки |
| `JAVA_OPTS` | Параметры JVM | Настройте под доступную память |

## Развертывание в Production

### 1. Подготовка сервера

```bash
# Установка Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Установка Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Настройка production конфигурации

Создайте `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: wallet-postgres-prod
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data_prod:/var/lib/postgresql/data
      - ./backup:/backup
    ports:
      - "127.0.0.1:5432:5432"  # Только локальный доступ
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 30s
      timeout: 10s
      retries: 3

  wallet-service:
    build: .
    container_name: wallet-service-prod
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      LOG_LEVEL: INFO
      JPA_SHOW_SQL: false
    ports:
      - "127.0.0.1:8080:8080"  # Только локальный доступ
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'
        reservations:
          memory: 512M
          cpus: '0.5'

volumes:
  postgres_data_prod:
    driver: local
```

### 3. Запуск production

```bash
# Загрузка переменных окружения
source .env

# Запуск production версии
docker-compose -f docker-compose.prod.yml up -d

# Проверка статуса
docker-compose -f docker-compose.prod.yml ps
```

## Развертывание в Docker Swarm

### 1. Инициализация Swarm

```bash
# Инициализация (на manager узле)
docker swarm init

# Присоединение worker узлов
docker swarm join --token <token> <manager-ip>:2377
```

### 2. Создание stack

```bash
# Деплой stack
docker stack deploy -c docker-compose.swarm.yml wallet-stack

# Проверка статуса
docker stack services wallet-stack
```

## Развертывание в Kubernetes

### 1. Создание namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: wallet-service
```

### 2. ConfigMap для конфигурации

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: wallet-config
  namespace: wallet-service
data:
  DB_URL: "jdbc:postgresql://postgres:5432/wallet_db"
  LOG_LEVEL: "INFO"
```

### 3. Secret для паролей

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: wallet-secrets
  namespace: wallet-service
type: Opaque
data:
  DB_PASSWORD: <base64-encoded-password>
  DB_USERNAME: <base64-encoded-username>
```

### 4. Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wallet-service
  namespace: wallet-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wallet-service
  template:
    metadata:
      labels:
        app: wallet-service
    spec:
      containers:
      - name: wallet-service
        image: wallet-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          valueFrom:
            configMapKeyRef:
              name: wallet-config
              key: DB_URL
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: wallet-secrets
              key: DB_PASSWORD
        resources:
          limits:
            memory: "1Gi"
            cpu: "1000m"
          requests:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## Мониторинг и логирование

### 1. Prometheus метрики

Добавьте в `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. Grafana дашборд

Создайте дашборд для мониторинга:
- JVM метрики
- HTTP запросы
- База данных
- Системные ресурсы

### 3. Централизованное логирование

```yaml
# docker-compose с ELK stack
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.8.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.8.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5044:5044"

  kibana:
    image: docker.elastic.co/kibana/kibana:8.8.0
    ports:
      - "5601:5601"
```

## Backup и восстановление

### 1. Автоматический backup PostgreSQL

```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup"
DB_NAME="wallet_db"
DB_USER="wallet_user"

docker exec wallet-postgres pg_dump -U $DB_USER $DB_NAME > $BACKUP_DIR/wallet_$DATE.sql

# Удаление старых backup файлов (старше 30 дней)
find $BACKUP_DIR -name "wallet_*.sql" -mtime +30 -delete
```

### 2. Cron job для backup

```bash
# Добавьте в crontab
0 2 * * * /path/to/backup.sh
```

### 3. Восстановление из backup

```bash
# Восстановление
docker exec -i wallet-postgres psql -U wallet_user -d wallet_db < backup_file.sql
```

## Безопасность

### 1. Firewall настройки

```bash
# UFW настройки
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP (если используете reverse proxy)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### 2. Reverse Proxy с Nginx

```nginx
# /etc/nginx/sites-available/wallet-service
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. SSL сертификат с Let's Encrypt

```bash
# Установка certbot
sudo apt install certbot python3-certbot-nginx

# Получение сертификата
sudo certbot --nginx -d your-domain.com

# Автоматическое обновление
sudo crontab -e
# Добавьте строку:
0 12 * * * /usr/bin/certbot renew --quiet
```

## Масштабирование

### 1. Горизонтальное масштабирование

```bash
# Docker Compose
docker-compose up -d --scale wallet-service=3

# Docker Swarm
docker service scale wallet-stack_wallet-service=5

# Kubernetes
kubectl scale deployment wallet-service --replicas=5
```

### 2. Load Balancer

```yaml
# Nginx конфигурация для балансировки
upstream wallet_backend {
    server 127.0.0.1:8080;
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
}

server {
    listen 80;
    location / {
        proxy_pass http://wallet_backend;
    }
}
```

## Troubleshooting

### 1. Проверка логов

```bash
# Docker Compose
docker-compose logs -f wallet-service

# Docker Swarm
docker service logs wallet-stack_wallet-service

# Kubernetes
kubectl logs -f deployment/wallet-service
```

### 2. Проверка ресурсов

```bash
# Использование ресурсов контейнеров
docker stats

# Проверка диска
df -h

# Проверка памяти
free -h
```

### 3. Перезапуск сервисов

```bash
# Docker Compose
docker-compose restart wallet-service

# Docker Swarm
docker service update --force wallet-stack_wallet-service

# Kubernetes
kubectl rollout restart deployment/wallet-service
```

## Обновление приложения

### 1. Zero-downtime deployment

```bash
# Docker Compose с health checks
docker-compose up -d --no-deps wallet-service

# Docker Swarm
docker service update --image wallet-service:new-version wallet-stack_wallet-service

# Kubernetes
kubectl set image deployment/wallet-service wallet-service=wallet-service:new-version
```

### 2. Rollback

```bash
# Docker Swarm
docker service update --image wallet-service:previous-version wallet-stack_wallet-service

# Kubernetes
kubectl rollout undo deployment/wallet-service
```
