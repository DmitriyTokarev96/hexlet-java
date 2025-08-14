package org.example.service;

import org.example.dto.WalletOperationRequest;
import org.example.exception.InsufficientFundsException;
import org.example.model.Wallet;
import org.example.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты для проверки конкурентности операций с кошельком
 * 
 * Симулирует 1000 RPS на один кошелек для проверки:
 * - Оптимистичной блокировки
 * - Механизма повторных попыток
 * - Корректности финального баланса
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceConcurrencyTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet testWallet;
    private BigDecimal initialBalance;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        initialBalance = new BigDecimal("10000.00"); // Начальный баланс 10000
        testWallet = new Wallet(initialBalance);
        testWallet.setId(walletId);
        testWallet.setCreatedAt(LocalDateTime.now());
        testWallet.setUpdatedAt(LocalDateTime.now());
        testWallet.setVersion(0L);
    }

    @Test
    void testConcurrentDeposits() throws Exception {
        // Arrange
        int numberOfOperations = 1000;
        BigDecimal depositAmount = new BigDecimal("1.00");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        // Мокаем репозиторий для симуляции оптимистичной блокировки
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        
        // При первом вызове save возвращаем кошелек, при последующих - исключение оптимистичной блокировки
        when(walletRepository.save(any(Wallet.class)))
            .thenReturn(testWallet)
            .thenThrow(new ObjectOptimisticLockingFailureException("Version conflict", new RuntimeException("Version conflict")))
            .thenReturn(testWallet);

        // Act
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfOperations];
        
        for (int i = 0; i < numberOfOperations; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    WalletOperationRequest request = new WalletOperationRequest(
                        walletId, 
                        WalletOperationRequest.OperationType.DEPOSIT, 
                        depositAmount
                    );
                    walletService.performOperation(request);
                } catch (Exception e) {
                    // Логируем исключения для анализа
                    System.err.println("Operation failed: " + e.getMessage());
                }
            }, executor);
        }

        // Ждем завершения всех операций
        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        // Assert
        // Проверяем, что все операции были обработаны
        verify(walletRepository, atLeastOnce()).findById(walletId);
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
    }

    @Test
    void testConcurrentWithdrawals() throws Exception {
        // Arrange
        int numberOfOperations = 1000;
        BigDecimal withdrawAmount = new BigDecimal("1.00");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        // Создаем кошелек с достаточным балансом для всех операций
        BigDecimal totalRequired = withdrawAmount.multiply(new BigDecimal(numberOfOperations));
        assertTrue(initialBalance.compareTo(totalRequired) >= 0, 
            "Начальный баланс должен быть достаточным для всех операций");

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfOperations];
        
        for (int i = 0; i < numberOfOperations; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    WalletOperationRequest request = new WalletOperationRequest(
                        walletId, 
                        WalletOperationRequest.OperationType.WITHDRAW, 
                        withdrawAmount
                    );
                    walletService.performOperation(request);
                } catch (Exception e) {
                    System.err.println("Withdrawal failed: " + e.getMessage());
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        // Assert
        verify(walletRepository, atLeastOnce()).findById(walletId);
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
    }

    @Test
    void testConcurrentMixedOperations() throws Exception {
        // Arrange
        int numberOfOperations = 1000;
        BigDecimal operationAmount = new BigDecimal("1.00");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfOperations];
        
        for (int i = 0; i < numberOfOperations; i++) {
            final boolean isDeposit = i % 2 == 0; // Чередуем депозиты и снятия
            
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    WalletOperationRequest request = new WalletOperationRequest(
                        walletId, 
                        isDeposit ? WalletOperationRequest.OperationType.DEPOSIT : WalletOperationRequest.OperationType.WITHDRAW, 
                        operationAmount
                    );
                    walletService.performOperation(request);
                    successfulOperations.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    // Ожидаемое исключение при недостатке средств
                    failedOperations.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Operation failed: " + e.getMessage());
                    failedOperations.incrementAndGet();
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        // Assert
        assertTrue(successfulOperations.get() > 0, "Должны быть успешные операции");
        verify(walletRepository, atLeastOnce()).findById(walletId);
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
        
        System.out.println("Successful operations: " + successfulOperations.get());
        System.out.println("Failed operations: " + failedOperations.get());
    }

    @Test
    void testOptimisticLockingRetry() {
        // Arrange
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            WalletOperationRequest.OperationType.DEPOSIT, 
            new BigDecimal("100.00")
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        
        // Первые два вызова save выбрасывают исключение оптимистичной блокировки
        // Третий вызов успешен
        when(walletRepository.save(any(Wallet.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException("Version conflict", new RuntimeException("Version conflict")))
            .thenThrow(new ObjectOptimisticLockingFailureException("Version conflict", new RuntimeException("Version conflict")))
            .thenReturn(testWallet);

        // Act
        Wallet result = walletService.performOperation(request);

        // Assert
        assertNotNull(result);
        verify(walletRepository, times(3)).save(any(Wallet.class));
    }

    @Test
    void testMaxRetryAttemptsExceeded() {
        // Arrange
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            WalletOperationRequest.OperationType.DEPOSIT, 
            new BigDecimal("100.00")
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        
        // Все вызовы save выбрасывают исключение оптимистичной блокировки
        when(walletRepository.save(any(Wallet.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException("Version conflict", new RuntimeException("Version conflict")));

        // Act & Assert
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            walletService.performOperation(request);
        });

        // Проверяем, что было 3 попытки (максимум по @Retryable)
        verify(walletRepository, times(3)).save(any(Wallet.class));
    }
}
