package com.example.digitalWalletApp.repository;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    // Fetch transactions for a user between two timestamps
    //List<Transaction> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND t.timestamp BETWEEN :start AND :end " +
            "ORDER BY t.timestamp DESC")
    List<Transaction> findByUserAndTimestampBetween(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
