package com.example.digitalWalletApp.model;

import jakarta.persistence.*;

@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double balance;

    @OneToOne // creates 1 to 1 relationship between user and wallet.
    @JoinColumn(name = "user_id", nullable = false) // nullable = false → Every wallet must be linked to a user; it cannot exist alone.
    private User user;

    public Wallet() {}

    public Wallet(User user) {
        this.user = user;
        this.balance = 0.0;
    }

    // Getters & Setters
    public Long getId() { return id; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
