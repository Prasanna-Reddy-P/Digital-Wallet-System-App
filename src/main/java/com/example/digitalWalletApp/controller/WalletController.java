package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.LoadMoneyResponse;
import com.example.digitalWalletApp.dto.TransferRequest;
import com.example.digitalWalletApp.dto.TransferResponse;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import com.example.digitalWalletApp.exception.UnauthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    // --------------------------------------------------------------------
    // Get Wallet Balance
    // --------------------------------------------------------------------
    @GetMapping("/balance")
    public ResponseEntity<LoadMoneyResponse> getBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching wallet balance request");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        Wallet wallet = walletService.getWallet(user);
        logger.info("User {} wallet balance fetched: {}", user.getEmail(), wallet.getBalance());

        LoadMoneyResponse response = walletService.toLoadMoneyResponse(wallet);
        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------------------
    // Get All Transactions
    // --------------------------------------------------------------------
    @GetMapping("/transactions")
    public ResponseEntity<List<?>> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching transactions request");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        List<?> transactions = walletService.getTransactions(user);
        logger.info("Fetched {} transactions for user {}", transactions.size(), user.getEmail());

        return ResponseEntity.ok(transactions);
    }

    // --------------------------------------------------------------------
    // Load Money (with unique transactionId)
    // --------------------------------------------------------------------
    @PostMapping("/load")
    public ResponseEntity<LoadMoneyResponse> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                       @RequestBody TransferRequest request) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        String transactionId = UUID.randomUUID().toString(); // ✅ unique txn ID
        logger.info("Wallet load request: user={}, amount={}, transactionId={}",
                user.getEmail(), request.getAmount(), transactionId);

        LoadMoneyResponse response = walletService.loadMoney(user, request.getAmount(), transactionId);

        logger.info("Wallet load successful: user={}, transactionId={}, newBalance={}",
                user.getEmail(), transactionId, response.getBalance());

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------------------
    // Transfer Money (with unique transactionId)
    // --------------------------------------------------------------------
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                     @RequestBody TransferRequest request) {
        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) throw new UnauthorizedException("Unauthorized access");

        String transactionId = UUID.randomUUID().toString(); // ✅ unique txn ID
        logger.info("Transfer request: sender={}, receiverId={}, amount={}, transactionId={}",
                sender.getEmail(), request.getReceiverId(), request.getAmount(), transactionId);

        TransferResponse response = walletService.transferAmount(
                sender,
                request.getReceiverId(),
                request.getAmount(),
                transactionId
        );

        logger.info("Transfer successful: sender={}, receiverId={}, amount={}, transactionId={}",
                sender.getEmail(), request.getReceiverId(), request.getAmount(), transactionId);

        return ResponseEntity.ok(response);
    }
}
