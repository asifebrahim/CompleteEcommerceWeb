package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.DeliveryOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryOtpDao extends JpaRepository<DeliveryOtp, Integer> {
    Optional<DeliveryOtp> findByOrderIdAndOtpCode(Integer orderId, String otpCode);
}
