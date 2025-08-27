package com.example.EcommerceFresh.Dao;

import java.util.List;

import com.example.EcommerceFresh.Entity.PaymentProof;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProofDao extends JpaRepository<PaymentProof,Integer> {
    List<PaymentProof> findByStatus(String status);

}
