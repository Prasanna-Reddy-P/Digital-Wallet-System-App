package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.TransactionDTO;
import com.example.digitalWalletApp.dto.UserInfoResponse;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final WalletService walletService;

    public AdminController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    // ------------------- Get all users -------------------
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Received request: GET /users");
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Access denied: Non-admin tried to access all users");
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        logger.info("Fetching all users...");
        List<User> users = walletService.getAllUsers();
        logger.info("Fetched {} users successfully", users.size());
        return ResponseEntity.ok(users);
    }

    // ------------------- Get user by ID -------------------
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                         @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}", userId);
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Access denied: Non-admin tried to access user {}", userId);
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        logger.info("Fetching user with ID {}", userId);
        User user = walletService.getUserById(userId);
        if (user == null) {
            logger.warn("User with ID {} not found", userId);
            return ResponseEntity.status(404).body("User not found");
        }

        Wallet wallet = walletService.getWallet(user);
        UserInfoResponse response = new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance());
        logger.info("User {} fetched successfully", userId);
        return ResponseEntity.ok(response);
    }

    // ------------------- Get transactions of a user -------------------
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                 @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/transactions", userId);
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Access denied: Non-admin tried to access transactions of user {}", userId);
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        logger.info("Fetching transactions for user {}", userId);
        User user = walletService.getUserById(userId);
        if (user == null) {
            logger.warn("User with ID {} not found", userId);
            return ResponseEntity.status(404).body("User not found");
        }

        List<TransactionDTO> transactions = walletService.getTransactions(user);
        logger.info("Fetched {} transactions for user {}", transactions.size(), userId);
        return ResponseEntity.ok(transactions);
    }

    // ------------------- Get full wallet of a user by ID -------------------
    @GetMapping("/users/{userId}/wallet")
    public ResponseEntity<?> getWalletByUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                               @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/wallet", userId);
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Access denied: Non-admin tried to access wallet of user {}", userId);
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        logger.info("Fetching wallet for user {}", userId);
        User user = walletService.getUserById(userId);
        if (user == null) {
            logger.warn("User with ID {} not found", userId);
            return ResponseEntity.status(404).body("User not found");
        }

        Wallet wallet = walletService.getWallet(user);
        logger.info("Wallet fetched successfully for user {}", userId);
        return ResponseEntity.ok(wallet);
    }

    // ------------------- Get balance of a user by ID -------------------
    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<?> getBalanceByUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/balance", userId);
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Access denied: Non-admin tried to access balance of user {}", userId);
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        logger.info("Fetching balance for user {}", userId);
        User user = walletService.getUserById(userId);
        if (user == null) {
            logger.warn("User with ID {} not found", userId);
            return ResponseEntity.status(404).body("User not found");
        }

        Wallet wallet = walletService.getWallet(user);
        logger.info("Balance fetched successfully for user {}: {}", userId, wallet.getBalance());
        return ResponseEntity.ok(wallet.getBalance());
    }
}
