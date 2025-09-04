package com.example.EcommerceFresh.dto;

import java.time.LocalDateTime;

public class DiscountDto {
    private Integer productId;
    private Double discountPrice;
    private Double discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer durationDays;
    private Integer durationHours;
    private Integer durationMinutes;

    // Constructors
    public DiscountDto() {}

    public DiscountDto(Integer productId, Double discountPrice, LocalDateTime startDate, LocalDateTime endDate) {
        this.productId = productId;
        this.discountPrice = discountPrice;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Helper method to calculate end date from duration
    public void calculateEndDateFromDuration() {
        if (startDate != null && (durationDays != null || durationHours != null || durationMinutes != null)) {
            LocalDateTime calculatedEndDate = startDate;
            
            if (durationDays != null && durationDays > 0) {
                calculatedEndDate = calculatedEndDate.plusDays(durationDays);
            }
            if (durationHours != null && durationHours > 0) {
                calculatedEndDate = calculatedEndDate.plusHours(durationHours);
            }
            if (durationMinutes != null && durationMinutes > 0) {
                calculatedEndDate = calculatedEndDate.plusMinutes(durationMinutes);
            }
            
            this.endDate = calculatedEndDate;
        }
    }

    // Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(Double discountPrice) { this.discountPrice = discountPrice; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
