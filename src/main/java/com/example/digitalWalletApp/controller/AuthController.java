package com.example.digitalWalletApp.controller;

import com.example.digitalWalletApp.config.JwtUtil;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }


        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // ✅ Set default role here
        user.setRole("USER");  // Default normal user

        User savedUser = userRepository.save(user);


        Wallet wallet = new Wallet(savedUser);
        walletRepository.save(wallet);


        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        response.put("name", savedUser.getName());
        response.put("email", savedUser.getEmail());
        response.put("balance", wallet.getBalance());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        User user = userRepository.findByEmail(credentials.get("email")).orElse(null);

        if (user == null || !passwordEncoder.matches(credentials.get("password"), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        Wallet wallet = walletRepository.findByUser(user).orElse(new Wallet(user));


        String token = jwtUtil.generateToken(user.getEmail());


        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful!");
        response.put("token", token);  // ✅ Send token
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("balance", wallet.getBalance());

        return ResponseEntity.ok(response);
    }
}
