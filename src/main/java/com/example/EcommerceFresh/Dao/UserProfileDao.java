package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.UserProfile;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileDao extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByUser(Users user);
}
