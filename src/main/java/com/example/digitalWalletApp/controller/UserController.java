package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.UserInfoResponse;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching user info");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) {
            logger.warn("Unauthorized access attempt");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Wallet wallet = walletService.getWallet(user);
        logger.info("User info fetched for email: {}, balance: {}", user.getEmail(), wallet.getBalance());

        return ResponseEntity.ok(new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance()));
    }
}
