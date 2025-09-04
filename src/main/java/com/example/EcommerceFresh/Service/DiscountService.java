package com.example.EcommerceFresh.Service;

import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Dao.ProductDiscountDao;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.ProductDiscount;
import com.example.EcommerceFresh.dto.DiscountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DiscountService {
    
    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);
    
    @Autowired
    private ProductDiscountDao discountDao;
    
    @Autowired
    private ProductDao productDao;

    // Create a new discount
    public ProductDiscount createDiscount(DiscountDto discountDto, String adminEmail) {
        Optional<Product> productOpt = productDao.findById(discountDto.getProductId());
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found with ID: " + discountDto.getProductId());
        }
        
        Product product = productOpt.get();
        
        // Check if product already has an active discount
        if (hasActiveDiscount(product.getId())) {
            throw new RuntimeException("Product already has an active discount. Please remove existing discount first.");
        }
        
        // Set start date to now if not provided
        if (discountDto.getStartDate() == null) {
            discountDto.setStartDate(LocalDateTime.now());
        }
        
        // Calculate end date from duration if provided
        discountDto.calculateEndDateFromDuration();
        
        // Validate end date
        if (discountDto.getEndDate() == null || discountDto.getEndDate().isBefore(discountDto.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }
        
        // Validate discount price
        if (discountDto.getDiscountPrice() == null || discountDto.getDiscountPrice() <= 0) {
            throw new RuntimeException("Discount price must be greater than 0");
        }
        
        if (discountDto.getDiscountPrice() >= product.getPrice()) {
            throw new RuntimeException("Discount price must be less than original price (₹" + product.getPrice() + ")");
        }
        
        ProductDiscount discount = new ProductDiscount(
            product,
            product.getPrice(),
            discountDto.getDiscountPrice(),
            discountDto.getStartDate(),
            discountDto.getEndDate(),
            adminEmail
        );
        
        logger.info("Creating discount for product ID {} by admin {}: ₹{} -> ₹{} ({}% off) from {} to {}", 
                   product.getId(), adminEmail, product.getPrice(), discountDto.getDiscountPrice(),
                   String.format("%.1f", discount.getDiscountPercentage()), 
                   discountDto.getStartDate(), discountDto.getEndDate());
        
        return discountDao.save(discount);
    }
    
    // Get active discount for a product
    public Optional<ProductDiscount> getActiveDiscount(Integer productId) {
        return discountDao.findActiveDiscountByProductId(productId, LocalDateTime.now());
    }
    
    // Check if product has active discount
    public boolean hasActiveDiscount(Integer productId) {
        return discountDao.hasActiveDiscount(productId, LocalDateTime.now());
    }
    
    // Get effective price (with discount if active)
    public double getEffectivePrice(Product product) {
        Optional<ProductDiscount> discount = getActiveDiscount(product.getId());
        return discount.map(ProductDiscount::getDiscountPrice).orElse(product.getPrice());
    }
    
    // Get discount info for a product (for display purposes)
    public ProductDiscount getDiscountInfo(Product product) {
        return getActiveDiscount(product.getId()).orElse(null);
    }
    
    // Get all active discounts
    public List<ProductDiscount> getAllActiveDiscounts() {
        return discountDao.findAllActiveDiscounts(LocalDateTime.now());
    }
    
    // Get all discounts for admin management
    public List<ProductDiscount> getAllDiscounts() {
        return discountDao.findAll();
    }
    
    // Get discounts created by specific admin
    public List<ProductDiscount> getDiscountsByAdmin(String adminEmail) {
        return discountDao.findByCreatedByOrderByCreatedAtDesc(adminEmail);
    }
    
    // Remove/deactivate discount
    public boolean removeDiscount(Integer discountId) {
        Optional<ProductDiscount> discountOpt = discountDao.findById(discountId);
        if (discountOpt.isPresent()) {
            ProductDiscount discount = discountOpt.get();
            discount.setIsActive(false);
            discountDao.save(discount);
            logger.info("Deactivated discount ID {} for product ID {}", discountId, discount.getProduct().getId());
            return true;
        }
        return false;
    }
    
    // Remove discount by product ID
    public boolean removeDiscountByProductId(Integer productId) {
        Optional<ProductDiscount> activeDiscount = getActiveDiscount(productId);
        if (activeDiscount.isPresent()) {
            return removeDiscount(activeDiscount.get().getId());
        }
        return false;
    }
    
    // Scheduled task to clean up expired discounts (runs every hour)
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void cleanupExpiredDiscounts() {
        List<ProductDiscount> expiredDiscounts = discountDao.findExpiredDiscounts(LocalDateTime.now());
        if (!expiredDiscounts.isEmpty()) {
            logger.info("Found {} expired discounts, deactivating them", expiredDiscounts.size());
            for (ProductDiscount discount : expiredDiscounts) {
                discount.setIsActive(false);
                discountDao.save(discount);
            }
            logger.info("Deactivated {} expired discounts", expiredDiscounts.size());
        }
    }
    
    // Get discount by ID
    public Optional<ProductDiscount> getDiscountById(Integer discountId) {
        return discountDao.findById(discountId);
    }
    
    // Update discount
    public ProductDiscount updateDiscount(Integer discountId, DiscountDto discountDto) {
        Optional<ProductDiscount> discountOpt = discountDao.findById(discountId);
        if (discountOpt.isEmpty()) {
            throw new RuntimeException("Discount not found with ID: " + discountId);
        }
        
        ProductDiscount discount = discountOpt.get();
        
        // Validate and update discount price
        if (discountDto.getDiscountPrice() != null) {
            if (discountDto.getDiscountPrice() <= 0) {
                throw new RuntimeException("Discount price must be greater than 0");
            }
            if (discountDto.getDiscountPrice() >= discount.getOriginalPrice()) {
                throw new RuntimeException("Discount price must be less than original price (₹" + discount.getOriginalPrice() + ")");
            }
            discount.setDiscountPrice(discountDto.getDiscountPrice());
        }
        
        // Update dates if provided
        if (discountDto.getStartDate() != null) {
            discount.setStartDate(discountDto.getStartDate());
        }
        
        if (discountDto.getEndDate() != null) {
            if (discountDto.getEndDate().isBefore(discount.getStartDate())) {
                throw new RuntimeException("End date must be after start date");
            }
            discount.setEndDate(discountDto.getEndDate());
        }
        
        return discountDao.save(discount);
    }
}
