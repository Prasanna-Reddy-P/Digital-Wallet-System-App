package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.TransferRequest;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.LimitConfig;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    // ---------------- Wallet endpoints for normal users ----------------

    @GetMapping("/balance")
    public ResponseEntity<?> getMyBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        return ResponseEntity.ok(Map.of("balance", walletService.getBalance(user)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getMyTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<?> transactions = walletService.getTransactions(user);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                       @RequestBody Map<String, Double> request) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        try {
            double amount = request.getOrDefault("amount", 0.0);
            walletService.loadMoney(user, amount);
            return ResponseEntity.ok(Map.of(
                    "message", "Wallet loaded successfully",
                    "balance", walletService.getBalance(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferAmount(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                            @RequestBody TransferRequest request) {
        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) return ResponseEntity.status(401).body("Unauthorized");

        try {
            walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
            return ResponseEntity.ok(Map.of(
                    "message", "Transfer successful",
                    "balance", walletService.getBalance(sender)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/limits")
    public ResponseEntity<?> getMyLimits(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Wallet wallet = walletService.getWallet(user);
        return ResponseEntity.ok(Map.of(
                "dailyLimit", wallet.getDailyLimit(),
                "monthlyLimit", wallet.getMonthlyLimit(),
                "balance", wallet.getBalance()
        ));
    }

    // ---------------- Admin endpoints for global limits ----------------

    @PostMapping("/admin/limits/global")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> setGlobalLimits(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                             @RequestBody Map<String, Double> request) {
        double daily = request.getOrDefault("dailyLimit", 100000.0);
        double monthly = request.getOrDefault("monthlyLimit", 500000.0);

        walletService.setGlobalLimits(daily, monthly);

        return ResponseEntity.ok(Map.of(
                "message", "Global limits updated",
                "dailyLimit", daily,
                "monthlyLimit", monthly
        ));
    }

    @GetMapping("/admin/limits/global")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getGlobalLimits(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        LimitConfig limits = walletService.getGlobalLimits();
        return ResponseEntity.ok(Map.of(
                "dailyLimit", limits.getDailyLimit(),
                "monthlyLimit", limits.getMonthlyLimit()
        ));
    }
}
