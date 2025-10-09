package com.example.digitalWalletApp.repository;

import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUser(User user); // 👈 Add this line
}
