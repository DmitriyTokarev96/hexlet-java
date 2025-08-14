package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для ответов об успешных операциях с кошельком
 */
public class OperationResponse {

    @JsonProperty("success")
    private boolean success = true;

    @JsonProperty("message")
    private String message;

    @JsonProperty("walletId")
    private UUID walletId;

    @JsonProperty("operationType")
    private String operationType;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("newBalance")
    private BigDecimal newBalance;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Конструкторы
    public OperationResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public OperationResponse(UUID walletId, String operationType, BigDecimal amount, BigDecimal newBalance, String message) {
        this();
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
        this.newBalance = newBalance;
        this.message = message;
    }

    // Геттеры и сеттеры
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OperationResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", walletId=" + walletId +
                ", operationType='" + operationType + '\'' +
                ", amount=" + amount +
                ", newBalance=" + newBalance +
                ", timestamp=" + timestamp +
                '}';
    }
}
