package com.example.digitalWalletApp.model;

import jakarta.persistence.*;
import org.antlr.v4.runtime.misc.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;



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

    @Min(value = 18, message = "User must be at least 18 years old")
    @Max(value = 100, message = "Age cannot be greater than 100")
    private Integer age;  // <-- Add this field

    // Default constructor
    public User() {}
    // Default constructor — required by JPA to create objects automatically.

    // Constructor without password
    // constructor overloading can be observed here
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

    // this means this particular column named role can not have null values.
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

    public Integer getAge() { return age; }// getter

    public void setAge(Integer age) { this.age = age; }  // setter
}