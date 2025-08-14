package org.example.service;

import org.example.dto.WalletOperationRequest;
import org.example.exception.InsufficientFundsException;
import org.example.exception.WalletNotFoundException;
import org.example.model.Wallet;
import org.example.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для WalletService
 * 
 * Использует Mockito для мокирования зависимостей
 * Тестирует бизнес-логику сервиса изолированно
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

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
        initialBalance = new BigDecimal("1000.00");
        testWallet = new Wallet(initialBalance);
        testWallet.setId(walletId);
        testWallet.setCreatedAt(LocalDateTime.now());
        testWallet.setUpdatedAt(LocalDateTime.now());
        testWallet.setVersion(0L);
    }

    @Test
    void testPerformOperation_Deposit() {
        // Arrange
        BigDecimal depositAmount = new BigDecimal("500.00");
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            WalletOperationRequest.OperationType.DEPOSIT, 
            depositAmount
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Wallet result = walletService.performOperation(request);

        // Assert
        assertNotNull(result);
        assertEquals(initialBalance.add(depositAmount), result.getBalance());
        verify(walletRepository).findById(walletId);
        verify(walletRepository).save(testWallet);
    }

    @Test
    void testPerformOperation_Withdraw() {
        // Arrange
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            WalletOperationRequest.OperationType.WITHDRAW, 
            withdrawAmount
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Wallet result = walletService.performOperation(request);

        // Assert
        assertNotNull(result);
        assertEquals(initialBalance.subtract(withdrawAmount), result.getBalance());
        verify(walletRepository).findById(walletId);
        verify(walletRepository).save(testWallet);
    }

    @Test
    void testPerformOperation_WithdrawInsufficientFunds() {
        // Arrange
        BigDecimal withdrawAmount = new BigDecimal("1500.00");
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            WalletOperationRequest.OperationType.WITHDRAW, 
            withdrawAmount
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        InsufficientFundsException exception = assertThrows(
            InsufficientFundsException.class,
            () -> walletService.performOperation(request)
        );

        assertEquals("Недостаточно средств для снятия 1500.00", exception.getMessage());
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testPerformOperation_WalletNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            nonExistentId, 
            WalletOperationRequest.OperationType.DEPOSIT, 
            new BigDecimal("100.00")
        );

        when(walletRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(
            WalletNotFoundException.class,
            () -> walletService.performOperation(request)
        );

        assertEquals("Кошелек с ID " + nonExistentId + " не найден", exception.getMessage());
        verify(walletRepository).findById(nonExistentId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testPerformOperation_InvalidOperationType() {
        // Arrange
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, 
            null, // Неверный тип операции
            new BigDecimal("100.00")
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> walletService.performOperation(request)
        );

        assertEquals("Неизвестный тип операции: null", exception.getMessage());
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testGetWallet_Success() {
        // Arrange
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        Wallet result = walletService.getWallet(walletId);

        // Assert
        assertNotNull(result);
        assertEquals(testWallet, result);
        verify(walletRepository).findById(walletId);
    }

    @Test
    void testGetWallet_NotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(walletRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        WalletNotFoundException exception = assertThrows(
            WalletNotFoundException.class,
            () -> walletService.getWallet(nonExistentId)
        );

        assertEquals("Кошелек с ID " + nonExistentId + " не найден", exception.getMessage());
        verify(walletRepository).findById(nonExistentId);
    }

    @Test
    void testCreateWallet_WithInitialBalance() {
        // Arrange
        BigDecimal initialBalance = new BigDecimal("500.00");
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Wallet result = walletService.createWallet(initialBalance);

        // Assert
        assertNotNull(result);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testCreateWallet_WithoutInitialBalance() {
        // Arrange
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Wallet result = walletService.createWallet();

        // Assert
        assertNotNull(result);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testCreateWallet_WithNullBalance() {
        // Arrange
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Wallet result = walletService.createWallet(null);

        // Assert
        assertNotNull(result);
        verify(walletRepository).save(any(Wallet.class));
    }
}
