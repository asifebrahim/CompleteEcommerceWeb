package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Users; // Import your custom Users entity
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository should be tied to the custom Users entity
public interface UserDao extends JpaRepository<Users, Integer> {

    // Change the method name to match the entity field "email"
    Optional<Users> findByEmail(String email);
}