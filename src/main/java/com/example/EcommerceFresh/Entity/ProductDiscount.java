package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_discounts")
public class ProductDiscount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(name = "discount_price")
    private Double discountPrice;

    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy; // Admin who created the discount

    // Constructors
    public ProductDiscount() {}

    public ProductDiscount(Product product, Double originalPrice, Double discountPrice, 
                          LocalDateTime startDate, LocalDateTime endDate, String createdBy) {
        this.product = product;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdBy = createdBy;
        calculateDiscountPercentage();
    }

    // Helper method to calculate discount percentage
    public void calculateDiscountPercentage() {
        if (originalPrice != null && discountPrice != null && originalPrice > 0) {
            this.discountPercentage = ((originalPrice - discountPrice) / originalPrice) * 100;
        }
    }

    // Helper method to check if discount is currently valid
    public boolean isCurrentlyActive() {
        if (!isActive) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    // Helper method to get time remaining
    public String getTimeRemaining() {
        if (!isCurrentlyActive()) return null;
        
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(now, endDate);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " " + hours + " hour" + (hours != 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours != 1 ? "s" : "") + " " + minutes + " minute" + (minutes != 1 ? "s" : "");
        } else {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { 
        this.originalPrice = originalPrice;
        calculateDiscountPercentage();
    }

    public Double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(Double discountPrice) { 
        this.discountPrice = discountPrice;
        calculateDiscountPercentage();
    }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
