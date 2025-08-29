package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_orders")
public class UserOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;
    
    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;
    
    @ManyToOne
    @JoinColumn(name = "order_group_id", referencedColumnName = "id")
    private OrderGroup orderGroup;
    
    @Column(name = "quantity")
    private Integer quantity = 1;
    
    @Column(name = "product_price_at_order")
    private Double productPriceAtOrder; // Store the price of the product when the order was placed
    
    @Column(name = "total_price")
    private Double totalPrice;
    
    @Column(name = "order_status")
    private String orderStatus = "Pending"; // Pending, Processing, Shipped, Delivered, Cancelled
    
    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }
    // legacy naming compatibility
    public Users getUsers() { return user; }
    public void setUsers(Users user) { this.user = user; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public OrderGroup getOrderGroup() { return orderGroup; }
    public void setOrderGroup(OrderGroup orderGroup) { this.orderGroup = orderGroup; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getProductPriceAtOrder() { return productPriceAtOrder; }
    public void setProductPriceAtOrder(Double productPriceAtOrder) { this.productPriceAtOrder = productPriceAtOrder; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    // legacy naming
    public String getStatus() { return this.orderStatus; }
    public void setStatus(String status) { this.orderStatus = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
