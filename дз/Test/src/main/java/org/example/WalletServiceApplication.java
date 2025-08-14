package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Главный класс Spring Boot приложения для управления кошельками
 * 
 * @EnableRetry - включает поддержку повторных попыток для оптимистичной блокировки
 */
@SpringBootApplication
@EnableRetry
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
