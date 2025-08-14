package org.example.exception;

/**
 * Исключение, выбрасываемое когда недостаточно средств для операции
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
