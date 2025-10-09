package com.example.digitalWalletApp.dto;

public class UserInfoResponse {
    private String name;
    private String email;
    private Double balance;

    public UserInfoResponse(String name, String email, Double balance) {
        this.name = name;
        this.email = email;
        this.balance = balance;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public Double getBalance() { return balance; }
}
