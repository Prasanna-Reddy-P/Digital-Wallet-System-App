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
    public ResponseEntity<LoadMoneyResponse> getBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("Fetching wallet balance request");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        Wallet wallet = walletService.getWallet(user);
        logger.info("User {} wallet balance fetched: {}", user.getEmail(), wallet.getBalance());

        LoadMoneyResponse response = walletService.toLoadMoneyResponse(wallet);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<?>> getTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        // Return type List<?> — in practice this is List<TransactionDTO>
        logger.info("Fetching transactions request");

        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        List<?> transactions = walletService.getTransactions(user);
        /*
        Calls walletService.getTransactions(user) which returns a list of DTOs (via TransactionMapper)
        and returns HTTP 200 OK with the list.
         */
        logger.info("Fetched {} transactions for user {}", transactions.size(), user.getEmail());

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/load")
    public ResponseEntity<LoadMoneyResponse> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                       @RequestBody TransferRequest request) {
        /*
        @RequestBody TransferRequest request → deserializes JSON request body into TransferRequest DTO
        (which contains amount and receiverId fields — here only amount is used for load).
         */
        User user = userService.getUserFromToken(authHeader);
        if (user == null) throw new UnauthorizedException("Unauthorized access");

        logger.info("Wallet load request: user={}, amount={}", user.getEmail(), request.getAmount());

        LoadMoneyResponse response = walletService.loadMoney(user, request.getAmount());
        logger.info("Wallet load successful: user={}, newBalance={}", user.getEmail(), response.getBalance());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                     @RequestBody TransferRequest request) {
        User sender = userService.getUserFromToken(authHeader);
        if (sender == null) throw new UnauthorizedException("Unauthorized access");

        logger.info("Transfer request: sender={}, receiverId={}, amount={}", sender.getEmail(), request.getReceiverId(), request.getAmount());

        TransferResponse response = walletService.transferAmount(sender, request.getReceiverId(), request.getAmount());
        logger.info("Transfer successful: sender={}, receiverId={}, amount={}", sender.getEmail(), request.getReceiverId(), request.getAmount());

        return ResponseEntity.ok(response);
    }
}

/*

2️⃣ ResponseEntity<LoadMoneyResponse>

This defines the type of HTTP response the method will return

ResponseEntity is a powerful class from Spring that allows you to:
Send HTTP status codes (like 200 OK, 404 Not Found, etc.)
Send headers (extra info like tokens, cache-control, etc.)
Send body (the actual data being returned — here it’s a LoadMoneyResponse object)


4️⃣ @RequestHeader(HttpHeaders.AUTHORIZATION)

This tells Spring:
“Take the Authorization header from the incoming HTTP request and inject its value into the authHeader variable.”

GET /api/wallet/balance
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Here,
HttpHeaders.AUTHORIZATION is a constant from Spring ("Authorization") — used for clarity instead of typing the string manually.

The value after Bearer is your JWT token.

So effectively,
👉 authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 */