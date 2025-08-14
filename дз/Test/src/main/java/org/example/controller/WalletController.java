package org.example.controller;

import org.example.dto.*;
import org.example.exception.InsufficientFundsException;
import org.example.exception.WalletNotFoundException;
import org.example.model.Wallet;
import org.example.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST контроллер для API кошельков
 * 
 * Предоставляет эндпоинты для:
 * - POST /api/v1/wallet - выполнение операций (депозит/снятие)
 * - GET /api/v1/wallets/{walletId} - получение баланса кошелька
 * - POST /api/v1/wallets - создание нового кошелька
 * 
 * Включает централизованную обработку ошибок через @ExceptionHandler
 */
@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Выполнение операции с кошельком (депозит или снятие)
     * 
     * @param request запрос на операцию
     * @return ответ с результатом операции
     */
    @PostMapping("/wallet")
    public ResponseEntity<OperationResponse> performOperation(@Valid @RequestBody WalletOperationRequest request) {
        Wallet wallet = walletService.performOperation(request);
        
        String operationType = request.getOperationType().getDescription();
        String message = String.format("Операция %s выполнена успешно", operationType);
        
        OperationResponse response = new OperationResponse(
            wallet.getId(),
            operationType,
            request.getAmount(),
            wallet.getBalance(),
            message
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о кошельке
     * 
     * @param walletId ID кошелька
     * @return информация о кошельке
     */
    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID walletId) {
        Wallet wallet = walletService.getWallet(walletId);
        
        WalletResponse response = new WalletResponse(
            wallet.getId(),
            wallet.getBalance(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Создание нового кошелька
     * 
     * @param initialBalance начальный баланс (опционально)
     * @return созданный кошелек
     */
    @PostMapping("/wallets")
    public ResponseEntity<WalletResponse> createWallet(@RequestParam(required = false) BigDecimal initialBalance) {
        Wallet wallet = initialBalance != null ? 
            walletService.createWallet(initialBalance) : 
            walletService.createWallet();
        
        WalletResponse response = new WalletResponse(
            wallet.getId(),
            wallet.getBalance(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Обработчики исключений

    /**
     * Обработка исключения "Кошелек не найден"
     */
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("WALLET_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Обработка исключения "Недостаточно средств"
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        ErrorResponse error = new ErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработка ошибок валидации
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String message = "Ошибка валидации: " + ex.getBindingResult().getFieldError().getDefaultMessage();
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Общий обработчик исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
