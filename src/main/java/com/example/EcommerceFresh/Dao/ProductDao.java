package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductDao extends JpaRepository<Product,Integer> {
    //...

    List<Product> findAllBycategoryId(int id);
    
    // Soft delete support: find only active products
    List<Product> findByActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(int categoryId);
    
    // Find all products including inactive ones (for admin)
    List<Product> findAll();
}
