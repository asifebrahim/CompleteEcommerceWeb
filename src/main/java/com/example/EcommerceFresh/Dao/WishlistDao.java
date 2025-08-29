package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Wishlist;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistDao extends JpaRepository<Wishlist, Integer> {
    List<Wishlist> findByUser(Users user);
    Wishlist findByUserAndProductId(Users user, Integer productId);
}
