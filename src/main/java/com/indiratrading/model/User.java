package com.indiratrading.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String clientScope;

    public User() {}

    public User(String userId, String name, String role, String clientScope) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.clientScope = clientScope;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getClientScope() { return clientScope; }
    public void setClientScope(String clientScope) { this.clientScope = clientScope; }
}
