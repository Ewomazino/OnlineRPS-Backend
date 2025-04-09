package com.propertyrental.model;

public class User {
    private String name;
    private String email;
    private String role;
    private String password;
    private String phone;
    private String accountType;

    // Constructor
    public User() {
    }

    // Getters and Setters
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPhone() {
        return phone;
    }
    public String getAccountType() {
        return accountType;
    }
}
