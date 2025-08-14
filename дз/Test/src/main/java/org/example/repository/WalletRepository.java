package org.example.repository;

import org.example.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для работы с кошельками
 * 
 * Расширяет JpaRepository, предоставляя базовые CRUD операции
 * Дополнительно включает методы для оптимистичной и пессимистичной блокировки
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Поиск кошелька по ID с оптимистичной блокировкой
     * @param id UUID кошелька
     * @return Optional с кошельком или пустой Optional
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithOptimisticLock(@Param("id") UUID id);

    /**
     * Поиск кошелька по ID с пессимистичной блокировкой
     * @param id UUID кошелька
     * @return Optional с кошельком или пустой Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("id") UUID id);

    /**
     * Проверка существования кошелька по ID
     * @param id UUID кошелька
     * @return true если кошелек существует, false иначе
     */
    boolean existsById(UUID id);
}
