package com.example.digitalWalletApp.repository;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    // Fetch transactions for a user between two timestamps
    List<Transaction> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);
}
