package com.example.digitalWalletApp.repository;

import com.example.digitalWalletApp.model.LimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LimitConfigRepository extends JpaRepository<LimitConfig, Long> {
    // Add this method
    Optional<LimitConfig> findTopByOrderByIdAsc();
}
