package com.example.EcommerceFresh.Service;

import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Retrieve the user using the Optional from the DAO
        Optional<Users> optionalUser = userDao.findByEmail(username);

        // Handle Optional logic: throw an exception if empty, otherwise map to UserDetails
        Users user = optionalUser.orElseThrow(() -> {
            // Logging for debugging purposes
            System.err.println("User not found: " + username);
            return new UsernameNotFoundException("User Not Found with username: " + username);
        });

        // Map user roles to GrantedAuthority
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName())) // role.getName() should match the role name
                .collect(Collectors.toSet());

        // Return Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(), // Ensure passwords are encoded in your database
                authorities
        );
    }
}