-- Скрипт инициализации PostgreSQL для Wallet Service
-- Выполняется автоматически при первом запуске контейнера

-- Создание расширения для работы с UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создание схемы (если не существует)
CREATE SCHEMA IF NOT EXISTS public;

-- Установка прав доступа
GRANT ALL ON SCHEMA public TO public;

-- Комментарий к базе данных
COMMENT ON DATABASE wallet_db IS 'База данных для сервиса управления кошельками';

-- Проверка и создание пользователя (если не существует)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'wallet_user') THEN
        CREATE ROLE wallet_user WITH LOGIN PASSWORD 'wallet_password';
    END IF;
END
$$;

-- Предоставление прав пользователю
GRANT CONNECT ON DATABASE wallet_db TO wallet_user;
GRANT USAGE ON SCHEMA public TO wallet_user;
GRANT CREATE ON SCHEMA public TO wallet_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO wallet_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO wallet_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO wallet_user;

-- Установка прав по умолчанию для будущих объектов
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO wallet_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO wallet_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO wallet_user;
