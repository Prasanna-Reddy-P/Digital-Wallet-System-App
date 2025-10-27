package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.TransactionDTO;
import com.example.digitalWalletApp.dto.UserInfoResponse;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import com.example.digitalWalletApp.exception.UnauthorizedException;
import com.example.digitalWalletApp.exception.ForbiddenException;
import com.example.digitalWalletApp.exception.UserNotFoundException;
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

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Received request: GET /users");

        User admin = userService.getUserFromToken(authHeader);
        if (admin == null) throw new UnauthorizedException("Unauthorized access");
        if (!"ADMIN".equals(admin.getRole())) throw new ForbiddenException("Admins only");

        logger.info("Fetching all users...");
        List<User> users = walletService.getAllUsers();
        logger.info("Fetched {} users successfully", users.size());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserInfoResponse> getUserById(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                        @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}", userId);

        User admin = userService.getUserFromToken(authHeader);
        if (admin == null) throw new UnauthorizedException("Unauthorized access");
        if (!"ADMIN".equals(admin.getRole())) throw new ForbiddenException("Admins only");

        User user = walletService.getUserById(userId);
        if (user == null) throw new UserNotFoundException("User not found with ID " + userId);

        Wallet wallet = walletService.getWallet(user);
        UserInfoResponse response = new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance());
        logger.info("User {} fetched successfully", userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<TransactionDTO>> getUserTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                                    @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/transactions", userId);

        User admin = userService.getUserFromToken(authHeader);
        if (admin == null) throw new UnauthorizedException("Unauthorized access");
        if (!"ADMIN".equals(admin.getRole())) throw new ForbiddenException("Admins only");

        User user = walletService.getUserById(userId);
        if (user == null) throw new UserNotFoundException("User not found with ID " + userId);

        List<TransactionDTO> transactions = walletService.getTransactions(user);
        logger.info("Fetched {} transactions for user {}", transactions.size(), userId);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/users/{userId}/wallet")
    public ResponseEntity<Wallet> getWalletByUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                    @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/wallet", userId);

        User admin = userService.getUserFromToken(authHeader);
        if (admin == null) throw new UnauthorizedException("Unauthorized access");
        if (!"ADMIN".equals(admin.getRole())) throw new ForbiddenException("Admins only");

        User user = walletService.getUserById(userId);
        if (user == null) throw new UserNotFoundException("User not found with ID " + userId);

        Wallet wallet = walletService.getWallet(user);
        logger.info("Wallet fetched successfully for user {}", userId);

        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<Double> getBalanceByUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                     @PathVariable Long userId) {
        logger.info("Received request: GET /users/{}/balance", userId);

        User admin = userService.getUserFromToken(authHeader);
        if (admin == null) throw new UnauthorizedException("Unauthorized access");
        if (!"ADMIN".equals(admin.getRole())) throw new ForbiddenException("Admins only");

        User user = walletService.getUserById(userId);
        if (user == null) throw new UserNotFoundException("User not found with ID " + userId);

        Wallet wallet = walletService.getWallet(user);
        logger.info("Balance fetched successfully for user {}: {}", userId, wallet.getBalance());

        return ResponseEntity.ok(wallet.getBalance());
    }
}

/*
The annotation @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader in a Spring-based application (or similar web framework)
indicates that the authHeader parameter of a method should be populated with the value of the Authorization header from the incoming HTTP request.
 */