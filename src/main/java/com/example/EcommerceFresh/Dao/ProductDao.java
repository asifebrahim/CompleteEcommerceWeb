package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductDao extends JpaRepository<Product,Integer> {
    //...

    List<Product> findAllBycategoryId(int id);
}
