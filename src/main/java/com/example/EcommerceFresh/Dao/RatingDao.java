package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.Rating;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingDao extends JpaRepository<Rating,Integer> {
    List<Rating> findByProduct(Product product);
    Optional<Rating> findByProductAndUsers(Product product, Users users);
    List<Rating> findByProductOrderByCreatedAtDesc(Product product);
    List<Rating> findByProductOrderByScoreDesc(Product product);
}
