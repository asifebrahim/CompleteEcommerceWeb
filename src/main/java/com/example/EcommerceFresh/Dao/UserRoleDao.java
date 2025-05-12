package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.UserRole;
import com.example.EcommerceFresh.Entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleDao extends JpaRepository<UserRole, UserRoleId> {
}
