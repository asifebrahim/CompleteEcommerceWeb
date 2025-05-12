package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "userRoles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "userId")
    private Users user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "roleId")
    private Roles role;

    public UserRole() {}

    public UserRole(Users user, Roles role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(user.getId(), role.getId());
    }

    // Getters and Setters
    public UserRoleId getId() { return id; }
    public void setId(UserRoleId id) { this.id = id; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public Roles getRole() { return role; }
    public void setRole(Roles role) { this.role = role; }
}
