package com.example.EcommerceFresh.Service;


import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Dao.RatingDao;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.Rating;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl {
    private ProductDao productDao;
    private RatingDao ratingDao;
    public ProductServiceImpl(ProductDao productDao, RatingDao ratingDao){
        this.productDao=productDao;
        this.ratingDao=ratingDao;
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
