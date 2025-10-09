package com.example.digitalWalletApp.dto;

public class TransferRequest {
    private Long receiverId;
    private Double amount;

    public TransferRequest() {}

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
