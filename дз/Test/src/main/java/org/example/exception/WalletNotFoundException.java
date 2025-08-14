package org.example.exception;

/**
 * Исключение, выбрасываемое когда кошелек не найден
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message) {
        super(message);
    }

    public WalletNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
