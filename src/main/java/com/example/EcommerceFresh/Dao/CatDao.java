package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatDao extends JpaRepository<Category,Integer> {
//    Category findByName(String name);
}
