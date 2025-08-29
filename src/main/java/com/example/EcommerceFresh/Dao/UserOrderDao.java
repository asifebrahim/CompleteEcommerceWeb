package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrderDao extends JpaRepository<UserOrder, Integer> {
    List<UserOrder> findByUserOrderByOrderDateDesc(Users user);
    List<UserOrder> findByUserAndOrderStatusOrderByOrderDateDesc(Users user, String orderStatus);
}
