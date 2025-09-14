package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductDao extends JpaRepository<Product,Integer> {
    //...

    List<Product> findAllBycategoryId(int id);
    
    // Soft delete support: find only active products (handling null as active)
    @Query("SELECT p FROM Product p WHERE p.active = true OR p.active IS NULL")
    List<Product> findByActiveTrue();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = ?1 AND (p.active = true OR p.active IS NULL)")
    List<Product> findByCategoryIdAndActiveTrue(int categoryId);
    
    // Find all products including inactive ones (for admin)
    List<Product> findAll();
}
