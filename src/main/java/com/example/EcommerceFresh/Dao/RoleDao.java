package com.example.EcommerceFresh.Dao;

import com.example.EcommerceFresh.Entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDao extends JpaRepository<Roles,Integer> {
    Roles findByName(String name);

}
