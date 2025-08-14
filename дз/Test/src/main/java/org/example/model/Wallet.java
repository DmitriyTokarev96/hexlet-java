package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA сущность для представления кошелька
 * 
 * Ключевые особенности:
 * - @Version для оптимистичной блокировки (решает проблемы конкурентности)
 * - BigDecimal для точных денежных расчетов
 * - UUID как первичный ключ
 * - Автоматическое управление временными метками
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallet_id", columnList = "id"),
    @Index(name = "idx_wallet_created_at", columnList = "created_at")
})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Баланс не может быть отрицательным")
    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Версия для оптимистичной блокировки
     * При каждом обновлении автоматически увеличивается
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Конструкторы
    public Wallet() {}

    public Wallet(BigDecimal initialBalance) {
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    // Методы для работы с балансом
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма депозита должна быть положительной");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма снятия должна быть положительной");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств для снятия");
        }
        this.balance = this.balance.subtract(amount);
    }

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
