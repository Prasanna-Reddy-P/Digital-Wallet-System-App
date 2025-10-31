package com.example.digitalWalletApp.dto;

public class LoadMoneyResponse {
    private Double balance;
    private Double dailySpent;
    private Double remainingDailyLimit;
    private Boolean frozen;
    private String message;

    // Getters & Setters
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance   = balance; }

    public Double getDailySpent() { return dailySpent; }
    public void setDailySpent(Double dailySpent) { this.dailySpent = dailySpent; }

    public Double getRemainingDailyLimit() { return remainingDailyLimit; }
    public void setRemainingDailyLimit(Double remainingDailyLimit) { this.remainingDailyLimit = remainingDailyLimit; }

    public Boolean getFrozen() { return frozen; }
    public void setFrozen(Boolean frozen) { this.frozen = frozen; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}