package com.requirements.backend.dto;

public class AuthResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private String assignedBusinessAnalystId;
    private String assignmentCode;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(
            String id,
            String name,
            String email,
            String role,
            String assignedBusinessAnalystId,
            String assignmentCode,
            String message) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.assignedBusinessAnalystId =
                assignedBusinessAnalystId;
        this.assignmentCode = assignmentCode;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}