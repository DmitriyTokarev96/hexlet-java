package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO для запросов операций с кошельком (депозит/снятие)
 * 
 * Валидация:
 * - walletId обязателен
 * - operationType обязателен
 * - amount должен быть положительным числом
 */
public class WalletOperationRequest {

    @NotNull(message = "ID кошелька обязателен")
    @JsonProperty("walletId")
    private UUID walletId;

    @NotNull(message = "Тип операции обязателен")
    @JsonProperty("operationType")
    private OperationType operationType;

    @NotNull(message = "Сумма обязательна")
    @Positive(message = "Сумма должна быть положительной")
    @JsonProperty("amount")
    private BigDecimal amount;

    // Конструкторы
    public WalletOperationRequest() {}

    public WalletOperationRequest(UUID walletId, OperationType operationType, BigDecimal amount) {
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
    }

    // Геттеры и сеттеры
    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Перечисление типов операций
     */
    public enum OperationType {
        DEPOSIT("Депозит"),
        WITHDRAW("Снятие");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "WalletOperationRequest{" +
                "walletId=" + walletId +
                ", operationType=" + operationType +
                ", amount=" + amount +
                '}';
    }
}
