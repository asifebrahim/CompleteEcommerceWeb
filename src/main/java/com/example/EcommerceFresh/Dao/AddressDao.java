package com.example.EcommerceFresh.Dao;

import java.util.Optional;

import com.example.EcommerceFresh.Entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressDao extends JpaRepository<Address,Integer> {
    Optional<Address> findByAddress1(String address1);
}
