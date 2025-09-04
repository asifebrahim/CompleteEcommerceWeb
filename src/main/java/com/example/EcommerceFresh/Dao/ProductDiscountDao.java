package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.ProductDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductDiscountDao extends JpaRepository<ProductDiscount, Integer> {
    
    // Find active discount for a product
    @Query("SELECT pd FROM ProductDiscount pd WHERE pd.product.id = :productId " +
           "AND pd.isActive = true AND pd.startDate <= :now AND pd.endDate > :now " +
           "ORDER BY pd.createdAt DESC LIMIT 1")
    Optional<ProductDiscount> findActiveDiscountByProductId(@Param("productId") Integer productId, @Param("now") LocalDateTime now);
    
    // Find all active discounts
    @Query("SELECT pd FROM ProductDiscount pd WHERE pd.isActive = true " +
           "AND pd.startDate <= :now AND pd.endDate > :now")
    List<ProductDiscount> findAllActiveDiscounts(@Param("now") LocalDateTime now);
    
    // Find all discounts for a product
    List<ProductDiscount> findByProductIdOrderByCreatedAtDesc(Integer productId);
    
    // Find discounts by product
    List<ProductDiscount> findByProduct(Product product);
    
    // Find expired discounts to clean up
    @Query("SELECT pd FROM ProductDiscount pd WHERE pd.isActive = true AND pd.endDate <= :now")
    List<ProductDiscount> findExpiredDiscounts(@Param("now") LocalDateTime now);
    
    // Check if product has any active discount
    @Query("SELECT COUNT(pd) > 0 FROM ProductDiscount pd WHERE pd.product.id = :productId " +
           "AND pd.isActive = true AND pd.startDate <= :now AND pd.endDate > :now")
    boolean hasActiveDiscount(@Param("productId") Integer productId, @Param("now") LocalDateTime now);
    
    // Find all discounts created by admin
    List<ProductDiscount> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
