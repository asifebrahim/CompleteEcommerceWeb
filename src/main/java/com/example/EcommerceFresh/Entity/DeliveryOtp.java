package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_otps")
public class DeliveryOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private UserOrder order;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used")
    private boolean used = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UserOrder getOrder() { return order; }
    public void setOrder(UserOrder order) { this.order = order; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
