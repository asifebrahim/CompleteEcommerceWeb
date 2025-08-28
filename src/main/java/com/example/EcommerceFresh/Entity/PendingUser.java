package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_users")
@Data
public class PendingUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String firstName;
    private String lastName;
    private String email;
    private String password; // already encoded

    private String otpHash; // store hashed otp
    private LocalDateTime otpExpiry;
    private LocalDateTime createdAt;

    public PendingUser() {}
}
