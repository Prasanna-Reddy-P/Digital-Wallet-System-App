package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.TransferRequest;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Request to get wallet balance");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access attempt to get balance");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Wallet wallet = walletService.getWallet(user);
        logger.info("Fetched balance for user {}: {}", user.getEmail(), wallet.getBalance());

        return ResponseEntity.ok(Map.of("balance", wallet.getBalance()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Request to get transactions");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access attempt to get transactions");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<?> transactions = walletService.getTransactions(user);
        logger.info("Fetched {} transactions for user {}", transactions.size(), user.getEmail());

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                       @RequestBody Map<String, Double> request) {
        double amount = request.getOrDefault("amount", 0.0);
        logger.info("Request to load money: amount={} ", amount);

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access attempt to load money");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            Map<String, Object> response = walletService.loadMoney(user, amount);
            logger.info("Money loaded successfully for user {}: new balance={}", user.getEmail(), response.get("balance"));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error loading money for user {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                      @RequestBody TransferRequest request) {
        logger.info("Request to transfer money: amount={} to receiverId={}", request.getAmount(), request.getReceiverId());

        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) {
            logger.warn("Unauthorized access attempt to transfer money");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            Map<String, Object> response = walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
            logger.info("Transfer successful from user {} to user {}: new balance={}", sender.getEmail(), request.getReceiverId(), response.get("balance"));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error transferring money for user {}: {}", sender.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
