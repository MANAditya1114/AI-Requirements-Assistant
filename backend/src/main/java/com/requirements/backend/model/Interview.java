package com.requirements.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "interviews")
public class Interview {

    @Id
    private String id;

    private String projectName;

    private String stakeholderName;

    /*
     * User account that owns this interview/project.
     */
    private String stakeholderUserId;

    /*
     * Business Analyst assigned to this interview/project.
     */
    private String businessAnalystUserId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private List<StakeholderResponse> responses = new ArrayList<>();

    private RequirementUnderstandingState understandingState;


    public Interview() {
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getProjectName() {
        return projectName;
    }


    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


    public String getStakeholderName() {
        return stakeholderName;
    }


    public void setStakeholderName(String stakeholderName) {
        this.stakeholderName = stakeholderName;
    }


    public String getStakeholderUserId() {
        return stakeholderUserId;
    }


    public void setStakeholderUserId(String stakeholderUserId) {
        this.stakeholderUserId = stakeholderUserId;
    }


    public String getBusinessAnalystUserId() {
        return businessAnalystUserId;
    }


    public void setBusinessAnalystUserId(String businessAnalystUserId) {
        this.businessAnalystUserId = businessAnalystUserId;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public List<StakeholderResponse> getResponses() {
        return responses;
    }


    public void setResponses(List<StakeholderResponse> responses) {
        this.responses = responses;
    }


    public RequirementUnderstandingState getUnderstandingState() {
        return understandingState;
    }


    public void setUnderstandingState(
            RequirementUnderstandingState understandingState) {

        this.understandingState = understandingState;
    }
}