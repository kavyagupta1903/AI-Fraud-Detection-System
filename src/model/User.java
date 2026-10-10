package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Domain model representing a system user (User or Admin).
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN,
        USER
    }

    private Long userId;
    private String username;
    private String passwordHash;
    private Role role;
    private String email;
    private Timestamp createdAt;

    public User() {
        this.createdAt = Timestamp.from(Instant.now());
    }

    public User(Long userId, String username, String passwordHash, Role role, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.email = email;
        this.createdAt = Timestamp.from(Instant.now());
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return String.format("User[id=%d, username='%s', role=%s]", userId, username, role);
    }
}
