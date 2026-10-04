package com.requirements.backend.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "requirements")
public class Requirement {

    @Id
    private String id;

    private String interviewId;

    private String projectName;

    private String text;

    /*
     * Stores the original requirement wording produced
     * by the AI before any stakeholder correction.
     *
     * This allows future AI analysis to recognize the
     * requirement even after the stakeholder changes
     * its displayed text.
     */
    private String originalText;

    private String type;

    private String status;

    private boolean stakeholderModified;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    public Requirement() {

        this.status = "DISCOVERED";
        this.stakeholderModified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public Requirement(
            String interviewId,
            String projectName,
            String text,
            String type) {

        this.interviewId = interviewId;
        this.projectName = projectName;

        this.text = text;

        /*
         * When the requirement is first discovered,
         * the current text is also its original AI text.
         */
        this.originalText = text;

        this.type = type;
        this.status = "DISCOVERED";
        this.stakeholderModified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getInterviewId() {
        return interviewId;
    }


    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }


    public String getProjectName() {
        return projectName;
    }


    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }


    public String getOriginalText() {
        return originalText;
    }


    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }


    public String getType() {
        return type;
    }


    public void setType(String type) {
        this.type = type;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public boolean isStakeholderModified() {
        return stakeholderModified;
    }


    public void setStakeholderModified(
            boolean stakeholderModified) {

        this.stakeholderModified = stakeholderModified;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}