package com.requirements.backend.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String role;

    // Used only for Stakeholder accounts.
    // Stores the MongoDB user ID of the Business Analyst
    // assigned to this Stakeholder.
    private String assignedBusinessAnalystId;

    // Used only for Business Analyst accounts.
    // Stakeholders use this code during registration
    // to connect to the correct Business Analyst.
    @Indexed(unique = true, sparse = true)
    private String assignmentCode;

    private LocalDateTime createdAt;

    public User() {
    }

    public User(
            String name,
            String email,
            String passwordHash,
            String role) {

        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAssignedBusinessAnalystId() {
        return assignedBusinessAnalystId;
    }

    public void setAssignedBusinessAnalystId(
            String assignedBusinessAnalystId) {

        this.assignedBusinessAnalystId =
                assignedBusinessAnalystId;
    }

    public String getAssignmentCode() {
        return assignmentCode;
    }

    public void setAssignmentCode(String assignmentCode) {
        this.assignmentCode = assignmentCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}