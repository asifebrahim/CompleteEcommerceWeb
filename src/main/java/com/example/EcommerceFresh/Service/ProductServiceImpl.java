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
        return productDao.findAll();
    }

    public void removeProductById(int id){
        // Delete associated payment proofs
        try {
            var productOpt = productDao.findById(id);
            if (productOpt.isPresent()) {
                var product = productOpt.get();
                var proofs = paymentProofDao.findByProduct(product);
                if (proofs != null) {
                    paymentProofDao.deleteAll(proofs);
                }
                // Delete user orders referencing this product
                var orders = userOrderDao.findAll();
                for (var ord : orders) {
                    if (ord.getProduct() != null && ord.getProduct().getId() == product.getId()) {
                        userOrderDao.delete(ord);
                    }
                }
                // Delete image file (best-effort)
                try {
                    java.nio.file.Path imagePath = java.nio.file.Paths.get(System.getProperty("user.dir"), "productImages", product.getImageName());
                    if (java.nio.file.Files.exists(imagePath)) {
                        java.nio.file.Files.delete(imagePath);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        productDao.deleteById(id);
    }

    public List<Product> getProductByCategoryId(int id){
        return  productDao.findAllBycategoryId(id);
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
