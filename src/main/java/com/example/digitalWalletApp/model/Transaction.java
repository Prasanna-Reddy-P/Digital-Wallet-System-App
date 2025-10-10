package com.example.digitalWalletApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;
    private String type; // "DEBIT" or "CREDIT"

    private LocalDateTime timestamp; // <-- renamed from createdAt for clarity

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Transaction() {}

    public Transaction(User user, Double amount, String type) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now(); // set automatically
    }

    // getters & setters
    public Long getId() { return id; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
