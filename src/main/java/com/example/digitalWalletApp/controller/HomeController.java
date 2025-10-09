package com.example.digitalWalletApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HomeController {
    @GetMapping
    public String home(){
        return "Hello Everyone I am Prasanna, Nice to see you";
    }
}
