package com.securebank.securebank.repository;

import com.securebank.securebank.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUser_Id(UUID userId);

    /** Fetches an account only if it belongs to the given user — ownership check in one query. */
    Optional<Account> findByIdAndUser_Id(UUID accountId, UUID userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    // ── Pessimistic-write locks for financial operations ──────────────────────

    /**
     * Acquires a row-level write lock (SELECT … FOR UPDATE) on the account,
     * verifying ownership in the same query.
     * Must be called inside an active {@code @Transactional} method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Account> findByIdAndUserIdForUpdate(
            @Param("id") UUID id,
            @Param("userId") UUID userId);

    /**
     * Acquires a row-level write lock on the account by account number.
     * Used to lock the target account in a transfer without an ownership check
     * (any active account is a valid transfer target).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber);
}
