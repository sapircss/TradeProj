package com.example.tradeproj.Models;

public class User {
    private String email;
    private String phone;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
}
