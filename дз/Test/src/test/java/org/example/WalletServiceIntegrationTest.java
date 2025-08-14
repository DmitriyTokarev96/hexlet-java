package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.WalletOperationRequest;
import org.example.dto.WalletResponse;
import org.example.model.Wallet;
import org.example.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для Wallet Service
 * 
 * Использует TestContainers для запуска реальной PostgreSQL в контейнере
 * Тестирует полный стек: контроллер -> сервис -> репозиторий -> база данных
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")

public class WalletServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        walletRepository.deleteAll();
    }

    @Test
    void testCreateWallet() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                .param("initialBalance", "1000.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.balance").value("1000.00"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testCreateWalletWithZeroBalance() throws Exception {
        mockMvc.perform(post("/api/v1/wallets"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value("0.00"));
    }

    @Test
    void testGetWallet() throws Exception {
        // Создаем кошелек
        Wallet wallet = walletRepository.save(new Wallet(new BigDecimal("500.00")));

        mockMvc.perform(get("/api/v1/wallets/{walletId}", wallet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wallet.getId().toString()))
                .andExpect(jsonPath("$.balance").value("500.00"));
    }

    @Test
    void testGetWalletNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/wallets/{walletId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("WALLET_NOT_FOUND"));
    }

    @Test
    void testDepositOperation() throws Exception {
        // Создаем кошелек
        Wallet wallet = walletRepository.save(new Wallet(new BigDecimal("100.00")));
        
        WalletOperationRequest request = new WalletOperationRequest(
            wallet.getId(), 
            WalletOperationRequest.OperationType.DEPOSIT, 
            new BigDecimal("50.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.walletId").value(wallet.getId().toString()))
                .andExpect(jsonPath("$.operationType").value("Депозит"))
                .andExpect(jsonPath("$.amount").value("50.00"))
                .andExpect(jsonPath("$.newBalance").value("150.00"));
    }

    @Test
    void testWithdrawOperation() throws Exception {
        // Создаем кошелек с достаточным балансом
        Wallet wallet = walletRepository.save(new Wallet(new BigDecimal("200.00")));
        
        WalletOperationRequest request = new WalletOperationRequest(
            wallet.getId(), 
            WalletOperationRequest.OperationType.WITHDRAW, 
            new BigDecimal("75.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operationType").value("Снятие"))
                .andExpect(jsonPath("$.amount").value("75.00"))
                .andExpect(jsonPath("$.newBalance").value("125.00"));
    }

    @Test
    void testWithdrawInsufficientFunds() throws Exception {
        // Создаем кошелек с недостаточным балансом
        Wallet wallet = walletRepository.save(new Wallet(new BigDecimal("50.00")));
        
        WalletOperationRequest request = new WalletOperationRequest(
            wallet.getId(), 
            WalletOperationRequest.OperationType.WITHDRAW, 
            new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void testOperationOnNonExistentWallet() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        WalletOperationRequest request = new WalletOperationRequest(
            nonExistentId, 
            WalletOperationRequest.OperationType.DEPOSIT, 
            new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WALLET_NOT_FOUND"));
    }

    @Test
    void testInvalidRequestValidation() throws Exception {
        // Отправляем запрос без обязательных полей
        String invalidJson = "{\"walletId\": null, \"amount\": -100}";

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
