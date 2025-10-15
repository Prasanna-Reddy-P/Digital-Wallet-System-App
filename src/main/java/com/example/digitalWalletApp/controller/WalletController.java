package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.LoadMoneyResponse;
import com.example.digitalWalletApp.dto.TransactionDTO;
import com.example.digitalWalletApp.dto.TransferRequest;
import com.example.digitalWalletApp.dto.TransferResponse;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final UserService userService;
    private final WalletService walletService;

    public WalletController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching wallet balance request");
        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized balance request");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Wallet wallet = walletService.getWallet(user);
        logger.info("User {} wallet balance fetched: {}", user.getEmail(), wallet.getBalance());

        LoadMoneyResponse response = new LoadMoneyResponse();
        response.setBalance(wallet.getBalance());
        response.setDailySpent(wallet.getDailySpent());
        response.setRemainingDailyLimit(walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Wallet balance fetched successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching transactions request");
        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized transactions request");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<TransactionDTO> transactions = walletService.getTransactions(user);
        logger.info("Fetched {} transactions for user {}", transactions.size(), user.getEmail());
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                       @RequestBody TransferRequest request) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized wallet load attempt");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        logger.info("Wallet load request: user={}, amount={}", user.getEmail(), request.getAmount());

        try {
            LoadMoneyResponse response = walletService.loadMoney(user, request.getAmount());
            logger.info("Wallet load successful: user={}, newBalance={}", user.getEmail(), response.getBalance());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Wallet wallet = walletService.getWallet(user);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();
            logger.warn("Wallet load failed for user {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    String.format("Error: %s | Remaining limit: %.2f | Frozen: %b", e.getMessage(), remainingLimit, wallet.getFrozen())
            );
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                      @RequestBody TransferRequest request) {
        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) {
            logger.warn("Unauthorized transfer attempt");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        logger.info("Transfer request: sender={}, receiverId={}, amount={}", sender.getEmail(), request.getReceiverId(), request.getAmount());

        try {
            TransferResponse response = walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
            logger.info("Transfer successful: sender={}, receiverId={}, amount={}", sender.getEmail(), request.getReceiverId(), request.getAmount());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Wallet wallet = walletService.getWallet(sender);
            double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();
            logger.warn("Transfer failed for sender {}: {}", sender.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    String.format("Error: %s | Remaining limit: %.2f | Frozen: %b", e.getMessage(), remainingLimit, wallet.getFrozen())
            );
        }
    }
}
