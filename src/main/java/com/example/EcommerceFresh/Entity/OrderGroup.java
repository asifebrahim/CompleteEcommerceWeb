package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "order_groups")
public class OrderGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;
    
    @Column(name = "group_status")
    private String groupStatus = "Pending"; // Pending, Processing, Shipped, Delivered, Cancelled
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "total_amount")
    private Double totalAmount;
    
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "transaction_id")
    private String transactionId;
    
    // Checkout details
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "mobile")
    private String mobile;
    
    @Column(name = "pin_code")
    private String pinCode;
    
    @Column(name = "town")
    private String town;
    
    @Column(name = "email_address")
    private String emailAddress;

    // One group can have multiple user orders
    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserOrder> userOrders;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public String getGroupStatus() { return groupStatus; }
    public void setGroupStatus(String groupStatus) { this.groupStatus = groupStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public List<UserOrder> getUserOrders() { return userOrders; }
    public void setUserOrders(List<UserOrder> userOrders) { this.userOrders = userOrders; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    
    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
}
