package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingUserDao extends JpaRepository<PendingUser, Integer> {
    Optional<PendingUser> findByEmail(String email);
    void deleteByEmail(String email);
    void deleteByOtpExpiryBefore(LocalDateTime time);
}
