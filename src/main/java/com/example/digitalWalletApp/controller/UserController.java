package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.dto.UserInfoResponse;
import com.example.digitalWalletApp.mapper.UserMapper;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.service.UserService;
import com.example.digitalWalletApp.service.WalletService;
import com.example.digitalWalletApp.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.digitalWalletApp.service.wallet.WalletFactory;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WalletFactory walletFactory;



    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching user info");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        Wallet wallet = walletFactory.getOrCreateWallet(user);
        logger.info("User info fetched for email: {}, balance: {}", user.getEmail(), wallet.getBalance());

        UserInfoResponse dto = userMapper.toDTO(user, wallet.getBalance());
        return ResponseEntity.ok(dto);

    }
}
