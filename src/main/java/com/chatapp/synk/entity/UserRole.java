package com.chatapp.synk.entity;

import com.chatapp.synk.enums.RoleName;
import jakarta.persistence.*;

@Entity
@Table(name = "user_roles", schema = "chatapp")
public class UserRole {
    public static final String ALIAS_USER_ROLE = "USRL";
    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    public UserRole(String id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public UserRole() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RoleName getName() {
        return name;
    }

    public void setname(RoleName name) {
        this.name = name;
    }
}