package com.example.EcommerceFresh.Entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Data
@Table(name="users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int Id;

    @Column(name="firstName")
    private String firstName;

    @Column(name="lastName")
    private String lastName;

    @Column(name="email")
    private String email;
    @Column(name="password")
    private String password;

    @ManyToMany(cascade =CascadeType.ALL , fetch=FetchType.EAGER)
    @JoinTable(
            name="userRoles",
            joinColumns = @JoinColumn(name="userId"),
            inverseJoinColumns = @JoinColumn(name="roleId")
    )
    private Set<Roles> roles;
    public Users(Users user){
        this.firstName=user.getFirstName();
        this.lastName=user.getLastName();
        this.email=user.getEmail();
        this.password=user.getPassword();
        this.roles=new HashSet<>(user.getRoles());
    }
    public Users(){

    }


}
