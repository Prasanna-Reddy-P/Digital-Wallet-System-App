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

import java.util.HashMap;
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
        logger.info("Received request to fetch wallet balance");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized attempt to access balance endpoint");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Wallet wallet = walletService.getWallet(user);
        double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();

        Map<String, Object> response = new HashMap<>();
        response.put("balance", wallet.getBalance());
        response.put("dailySpent", wallet.getDailySpent());
        response.put("frozen", wallet.getFrozen());
        response.put("remainingDailyLimit", Math.max(0, remainingLimit));

        logger.info("Fetched balance for user {}: {}", user.getEmail(), wallet.getBalance());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Received request to fetch transaction history");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized attempt to fetch transactions");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<?> transactions = walletService.getTransactions(user);
        logger.info("User {} fetched {} transactions", user.getEmail(), transactions.size());

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                       @RequestBody Map<String, Double> request) {
        double amount = request.getOrDefault("amount", 0.0);
        logger.info("Received wallet load request: amount={}", amount);

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized attempt to load money");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Map<String, Object> response = walletService.loadMoney(user, amount);
            Wallet wallet = walletService.getWallet(user);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();

            // Add remainingDailyLimit and frozen info
            Map<String, Object> mutableResponse = new HashMap<>(response);
            mutableResponse.put("remainingDailyLimit", Math.max(0, remainingLimit));
            mutableResponse.put("frozen", wallet.getFrozen());

            logger.info("Money successfully loaded for user {}: newBalance={}", user.getEmail(), wallet.getBalance());

            return ResponseEntity.ok(mutableResponse);
        } catch (IllegalArgumentException e) {
            Wallet wallet = walletService.getWallet(user);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();

            // Include remaining limit and frozen even on failure
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
//            errorResponse.put("minAmount", walletService.getWalletProperties().getMinAmount());
//            errorResponse.put("maxAmount", walletService.getWalletProperties().getMaxAmount());
            errorResponse.put("remainingDailyLimit", Math.max(0, remainingLimit));
            errorResponse.put("frozen", wallet.getFrozen());

            logger.warn("Failed to load money for user {}: {}", user.getEmail(), e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                      @RequestBody TransferRequest request) {
        logger.info("Received transfer request: receiverId={}, amount={}", request.getReceiverId(), request.getAmount());

        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) {
            logger.warn("Unauthorized attempt to transfer funds");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Map<String, Object> response = walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
            Wallet senderWallet = walletService.getWallet(sender);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - senderWallet.getDailySpent();

            Map<String, Object> mutableResponse = new HashMap<>(response);
            mutableResponse.put("remainingDailyLimit", Math.max(0, remainingLimit));
            mutableResponse.put("frozen", senderWallet.getFrozen());

            logger.info("Transfer completed successfully for sender {} to receiverId={}", sender.getEmail(), request.getReceiverId());

            return ResponseEntity.ok(mutableResponse);
        } catch (IllegalArgumentException e) {
            Wallet senderWallet = walletService.getWallet(sender);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - senderWallet.getDailySpent();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
//            errorResponse.put("minAmount", walletService.getWalletProperties().getMinAmount());
//            errorResponse.put("maxAmount", walletService.getWalletProperties().getMaxAmount());
            errorResponse.put("remainingDailyLimit", Math.max(0, remainingLimit));
            errorResponse.put("frozen", senderWallet.getFrozen());

            logger.warn("Transfer failed for sender {}: {}", sender.getEmail(), e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
