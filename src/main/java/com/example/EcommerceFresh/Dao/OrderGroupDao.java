package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.OrderGroup;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderGroupDao extends JpaRepository<OrderGroup, Integer> {
    List<OrderGroup> findByUserOrderByCreatedAtDesc(Users user);
    List<OrderGroup> findByGroupStatus(String groupStatus);
}
