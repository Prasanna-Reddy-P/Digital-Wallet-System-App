package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.config.JwtUtil;
import com.example.digitalWalletApp.dto.TransferRequest;
import com.example.digitalWalletApp.dto.UserInfoResponse;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtUtil jwtUtil;


    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Wallet wallet = walletRepository.findByUser(user).orElse(new Wallet(user));
        return ResponseEntity.ok(new UserInfoResponse(user.getName(), user.getEmail(), wallet.getBalance()));
    }


    @GetMapping("/me/balance")
    public ResponseEntity<?> getMyBalance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Wallet wallet = walletRepository.findByUser(user).orElse(new Wallet(user));
        return ResponseEntity.ok(Map.of("balance", wallet.getBalance()));
    }


    @GetMapping("/me/transactions")
    public ResponseEntity<?> getMyTransactions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Transaction> transactions = transactionRepository.findByUser(user);
        return ResponseEntity.ok(transactions);
    }


    @PostMapping("/transfer")
    public ResponseEntity<?> transferAmount(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                            @RequestBody TransferRequest request) {
        User sender = getUserFromToken(authHeader);
        if (sender == null) return ResponseEntity.status(401).body("Unauthorized");

        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);
        if (receiver == null) return ResponseEntity.badRequest().body("Receiver not found");

        Wallet senderWallet = walletRepository.findByUser(sender).orElse(new Wallet(sender));
        Wallet receiverWallet = walletRepository.findByUser(receiver).orElse(new Wallet(receiver));

        if (senderWallet.getBalance() < request.getAmount())
            return ResponseEntity.badRequest().body("Insufficient balance");


        senderWallet.setBalance(senderWallet.getBalance() - request.getAmount());
        receiverWallet.setBalance(receiverWallet.getBalance() + request.getAmount());

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);


        transactionRepository.save(new Transaction(sender, -request.getAmount(), "DEBIT"));
        transactionRepository.save(new Transaction(receiver, request.getAmount(), "CREDIT"));

        return ResponseEntity.ok(Map.of("message", "Transfer successful", "balance", senderWallet.getBalance()));
    }


    @PostMapping("/me/load")
    public ResponseEntity<?> loadMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                       @RequestBody Map<String, Double> request) {
        if (!request.containsKey("amount") || request.get("amount") <= 0)
            return ResponseEntity.badRequest().body("Amount must be > 0");

        double amount = request.get("amount");
        User user = getUserFromToken(authHeader);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Wallet wallet = walletRepository.findByUser(user).orElse(new Wallet(user));
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new Transaction(user, amount, "SELF_CREDITED"));

        return ResponseEntity.ok(Map.of(
                "message", "Wallet loaded successfully",
                "balance", wallet.getBalance()
        ));
    }

}
