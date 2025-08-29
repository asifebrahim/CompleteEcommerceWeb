package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestDao extends JpaRepository<ReturnRequest, Integer> {
    List<ReturnRequest> findByStatus(String status);
}
