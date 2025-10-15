package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.LoadMoneyResponse;
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

    /** GET /balance **/
    @GetMapping("/balance")
    public ResponseEntity<LoadMoneyResponse> getBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching wallet balance");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access to /balance");
            return ResponseEntity.status(401).build();
        }

        Wallet wallet = walletService.getWallet(user);
        double remainingLimit = walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent();

        LoadMoneyResponse response = new LoadMoneyResponse();
        response.setBalance(wallet.getBalance());
        response.setDailySpent(wallet.getDailySpent());
        response.setRemainingDailyLimit(Math.max(0, remainingLimit));
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Wallet fetched successfully");

        logger.info("Wallet balance for user {}: {}", user.getEmail(), wallet.getBalance());
        return ResponseEntity.ok(response);
    }

    /** GET /transactions **/
    @GetMapping("/transactions")
    public ResponseEntity<List<?>> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching transaction history");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access to /transactions");
            return ResponseEntity.status(401).build();
        }

        List<?> transactions = walletService.getTransactions(user);
        logger.info("User {} has {} transactions", user.getEmail(), transactions.size());
        return ResponseEntity.ok(transactions);
    }

    /** POST /load **/
    @PostMapping("/load")
    public ResponseEntity<LoadMoneyResponse> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                       @RequestBody TransferRequest request) {
        double amount = request.getAmount();
        logger.info("Load money request: amount={}", amount);

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized load money attempt");
            return ResponseEntity.status(401).build();
        }

        try {
            LoadMoneyResponse response = walletService.loadMoney(user, amount);
            Wallet wallet = walletService.getWallet(user);
            response.setRemainingDailyLimit(Math.max(0, walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent()));
            response.setFrozen(wallet.getFrozen());
            logger.info("Money loaded successfully for user {}: newBalance={}", user.getEmail(), wallet.getBalance());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to load money for user {}: {}", user.getEmail(), e.getMessage());
            Wallet wallet = walletService.getWallet(user);
            LoadMoneyResponse errorResponse = new LoadMoneyResponse();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setRemainingDailyLimit(Math.max(0, walletService.getWalletProperties().getDailyLimit() - wallet.getDailySpent()));
            errorResponse.setFrozen(wallet.getFrozen());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /** POST /transfer **/
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                     @RequestBody TransferRequest request) {
        logger.info("Transfer request: receiverId={}, amount={}", request.getReceiverId(), request.getAmount());

        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) {
            logger.warn("Unauthorized transfer attempt");
            return ResponseEntity.status(401).build();
        }

        try {
            TransferResponse response = walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
            Wallet senderWallet = walletService.getWallet(sender);
            response.setRemainingDailyLimit(Math.max(0, walletService.getWalletProperties().getDailyLimit() - senderWallet.getDailySpent()));
            response.setFrozen(senderWallet.getFrozen());
            logger.info("Transfer successful: sender {} -> recipientId={}, amount={}", sender.getEmail(), request.getReceiverId(), request.getAmount());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Transfer failed for sender {}: {}", sender.getEmail(), e.getMessage());
            Wallet senderWallet = walletService.getWallet(sender);
            TransferResponse errorResponse = new TransferResponse();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setRemainingDailyLimit(Math.max(0, walletService.getWalletProperties().getDailyLimit() - senderWallet.getDailySpent()));
            errorResponse.setFrozen(senderWallet.getFrozen());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
