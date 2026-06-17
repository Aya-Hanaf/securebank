package com.securebank.securebank.repository;

import com.securebank.securebank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Lists all transactions for an account, verifying the account belongs to the user.
     * Sorted newest-first.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.account.id = :accountId
              AND t.account.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findByAccountIdAndUserId(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId);

    /**
     * Fetches a single transaction, verifying the owning account belongs to the user.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.id = :id
              AND t.account.user.id = :userId
            """)
    Optional<Transaction> findByIdAndUserId(
            @Param("id") UUID id,
            @Param("userId") UUID userId);

    boolean existsByReference(String reference);
}
