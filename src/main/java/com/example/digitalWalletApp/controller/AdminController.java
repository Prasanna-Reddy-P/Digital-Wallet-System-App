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
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        List<User> users = walletService.getAllUsers(); // We'll add this in WalletService
        return ResponseEntity.ok(users);
    }

    // ------------------- Get user by ID -------------------
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                         @PathVariable Long userId) {
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        User user = walletService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        Wallet wallet = walletService.getWallet(user);
        UserInfoResponse response = new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance());
        return ResponseEntity.ok(response);
    }

    // ------------------- Get transactions of a user -------------------
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                 @PathVariable Long userId) {
        User admin = userService.getUserFromToken(authHeader);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(403).body("Forbidden: Admins only");
        }

        User user = walletService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        List<TransactionDTO> transactions = walletService.getTransactions(user);
        return ResponseEntity.ok(transactions);
    }
}
