package com.example.EcommerceFresh.Dao;

import java.util.List;

import com.example.EcommerceFresh.Entity.PaymentProof;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProofDao extends JpaRepository<PaymentProof,Integer> {
    List<PaymentProof> findByStatus(String status);
    List<PaymentProof> findByProduct(com.example.EcommerceFresh.Entity.Product product);

    // Search helpers
    List<PaymentProof> findByTransactionIdContainingIgnoreCase(String tx);
    List<PaymentProof> findByUsers_EmailContainingIgnoreCase(String email);
    List<PaymentProof> findByTransactionIdContainingIgnoreCaseOrUsers_EmailContainingIgnoreCase(String tx, String email);

}
