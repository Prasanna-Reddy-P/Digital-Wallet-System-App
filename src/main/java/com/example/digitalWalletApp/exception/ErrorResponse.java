package com.example.digitalWalletApp.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {
    private LocalDateTime timestamp;
    private int errorCode;
    private String message;
    private Map<String, String> validationErrors;

    public ErrorResponse(LocalDateTime timestamp, int errorCode, String message, Map<String, String> validationErrors) {
        this.timestamp = timestamp;
        this.errorCode = errorCode;
        this.message = message;
        this.validationErrors = validationErrors;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
}
