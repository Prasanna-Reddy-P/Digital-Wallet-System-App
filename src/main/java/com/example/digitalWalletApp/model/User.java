package com.example.digitalWalletApp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key

    private String name;
    private String email;

    @Column(nullable = false)
    private String password;

    // Default constructor
    public User() {}

    // Constructor without password
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Constructor with password
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Column(nullable = false)
    private String role = "USER"; // Default is normal user

    // Getter & Setter
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }


    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}