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

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = userService.getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Wallet wallet = walletService.getWallet(user);
        return ResponseEntity.ok(new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance()));
    }
}
