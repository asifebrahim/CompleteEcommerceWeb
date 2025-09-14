package com.example.EcommerceFresh.Service;


import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Dao.RatingDao;
import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.Rating;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl {
    private ProductDao productDao;
    private RatingDao ratingDao;
    private PaymentProofDao paymentProofDao;
    private UserOrderDao userOrderDao;
    public ProductServiceImpl(ProductDao productDao, RatingDao ratingDao, PaymentProofDao paymentProofDao, UserOrderDao userOrderDao){
        this.productDao=productDao;
        this.ratingDao=ratingDao;
        this.paymentProofDao = paymentProofDao;
        this.userOrderDao = userOrderDao;
    }

    public void Save(Product tempProduct){
        productDao.save(tempProduct);
    }
    public Optional<Product> findById(int id){
        Optional<Product> ans=productDao.findById(id);
        return ans;
    }

    public List<Product> findAllProduct(){
        return productDao.findByActiveTrue(); // Only return active products
    }
    
    public List<Product> findAllProductsIncludingInactive(){
        return productDao.findAll(); // For admin use - shows all products
    }

    public void removeProductById(int id){
        // Soft delete: mark product as inactive instead of physical deletion
        try {
            var productOpt = productDao.findById(id);
            if (productOpt.isPresent()) {
                var product = productOpt.get();
                product.setActive(false); // Mark as inactive
                productDao.save(product); // Save the change
                System.out.println("Product " + id + " marked as inactive (soft deleted)");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to deactivate product: " + ex.getMessage());
        }
    }

    public void reactivateProductById(int id){
        // Reactivate product: mark product as active
        try {
            var productOpt = productDao.findById(id);
            if (productOpt.isPresent()) {
                var product = productOpt.get();
                product.setActive(true); // Mark as active
                productDao.save(product); // Save the change
                System.out.println("Product " + id + " reactivated");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to reactivate product: " + ex.getMessage());
        }
    }

    public List<Product> getProductByCategoryId(int id){
        return productDao.findByCategoryIdAndActiveTrue(id); // Only active products by category
    }

    public double getAverageRating(Product product){
        var ratings = ratingDao.findByProduct(product);
        if(ratings == null || ratings.isEmpty()) return 0.0;
        return ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
    }

    public void saveRating(Rating rating){
        ratingDao.save(rating);
    }

    public List<Rating> getRatingsForProduct(Product product){
        return ratingDao.findByProductOrderByCreatedAtDesc(product);
    }

    public List<Rating> getRatingsForProductSortedByScore(Product product){
        return ratingDao.findByProductOrderByScoreDesc(product);
    }
}
