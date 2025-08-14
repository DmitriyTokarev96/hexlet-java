package org.example.service;

import org.example.dto.WalletOperationRequest;
import org.example.exception.InsufficientFundsException;
import org.example.exception.WalletNotFoundException;
import org.example.model.Wallet;
import org.example.repository.WalletRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Сервис для работы с кошельками
 * 
 * Ключевые особенности:
 * - @Transactional для атомарности операций
 * - @Retryable для автоматических повторных попыток при конфликтах оптимистичной блокировки
 * - Обработка конкурентных запросов через @Version в сущности Wallet
 */
@Service
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Выполнение операции с кошельком (депозит или снятие)
     * 
     * @param request запрос на операцию
     * @return обновленный кошелек
     * @throws WalletNotFoundException если кошелек не найден
     * @throws InsufficientFundsException если недостаточно средств для снятия
     * 
     * @Retryable автоматически повторяет операцию при ObjectOptimisticLockingFailureException
     * Это решает проблему конкурентности - если несколько запросов одновременно
     * пытаются изменить один кошелек, они будут автоматически повторены
     */
    @Retryable(
        value = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Wallet performOperation(WalletOperationRequest request) {
        UUID walletId = request.getWalletId();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Кошелек с ID " + walletId + " не найден"));

        BigDecimal amount = request.getAmount();

        switch (request.getOperationType()) {
            case DEPOSIT:
                wallet.deposit(amount);
                break;
            case WITHDRAW:
                if (wallet.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Недостаточно средств для снятия " + amount);
                }
                wallet.withdraw(amount);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип операции: " + request.getOperationType());
        }

        return walletRepository.save(wallet);
    }

    /**
     * Получение информации о кошельке
     * 
     * @param walletId ID кошелька
     * @return кошелек
     * @throws WalletNotFoundException если кошелек не найден
     */
    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Кошелек с ID " + walletId + " не найден"));
    }

    /**
     * Создание нового кошелька
     * 
     * @param initialBalance начальный баланс
     * @return созданный кошелек
     */
    public Wallet createWallet(BigDecimal initialBalance) {
        Wallet wallet = new Wallet(initialBalance);
        return walletRepository.save(wallet);
    }

    /**
     * Создание кошелька с нулевым балансом
     * 
     * @return созданный кошелек
     */
    public Wallet createWallet() {
        return createWallet(BigDecimal.ZERO);
    }
}
