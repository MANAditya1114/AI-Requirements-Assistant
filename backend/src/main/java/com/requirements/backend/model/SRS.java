package com.requirements.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "srs_documents")
public class SRS {

    @Id
    private String id;

    private String interviewId;
    private String projectName;
    private String stakeholderName;

    // =====================================================
    // Main SRS Sections
    // =====================================================

    private String introduction;
    private String purpose;
    private String scope;
    private String systemOverview;

    private List<String> userRoleRequirements =
            new ArrayList<>();

    private List<String> functionalRequirements =
            new ArrayList<>();

    private List<String> nonFunctionalRequirements =
            new ArrayList<>();

    private List<String> dataRequirements =
            new ArrayList<>();

    private List<String> securityRequirements =
            new ArrayList<>();

    private List<String> performanceRequirements =
            new ArrayList<>();

    private List<String> notificationRequirements =
            new ArrayList<>();

    private List<String> errorHandlingRequirements =
            new ArrayList<>();

    // =====================================================
    // Requirements Engineering Analysis
    // =====================================================

    private List<RequirementUnderstandingState.CoverageItem> coverage =
            new ArrayList<>();

    private List<RequirementUnderstandingState.AmbiguityItem> ambiguities =
            new ArrayList<>();

    private List<String> missingInformation =
            new ArrayList<>();

    // =====================================================
    // Validation Summary
    // =====================================================

    private int totalRequirements;
    private int functionalRequirementCount;
    private int nonFunctionalRequirementCount;
    private int validatedRequirementCount;

    private String conclusion;

    // GENERATED / REVIEWED
    private String status;

    private LocalDateTime generatedAt;
    private LocalDateTime updatedAt;


    public SRS() {
        this.status = "GENERATED";
        this.generatedAt = LocalDateTime.now();
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


    public String getStakeholderName() {
        return stakeholderName;
    }

    public void setStakeholderName(String stakeholderName) {
        this.stakeholderName = stakeholderName;
    }


    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }


    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }


    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


    public String getSystemOverview() {
        return systemOverview;
    }

    public void setSystemOverview(String systemOverview) {
        this.systemOverview = systemOverview;
    }


    public List<String> getUserRoleRequirements() {
        return userRoleRequirements;
    }

    public void setUserRoleRequirements(
            List<String> userRoleRequirements) {

        this.userRoleRequirements =
                userRoleRequirements;
    }


    public List<String> getFunctionalRequirements() {
        return functionalRequirements;
    }

    public void setFunctionalRequirements(
            List<String> functionalRequirements) {

        this.functionalRequirements =
                functionalRequirements;
    }


    public List<String> getNonFunctionalRequirements() {
        return nonFunctionalRequirements;
    }

    public void setNonFunctionalRequirements(
            List<String> nonFunctionalRequirements) {

        this.nonFunctionalRequirements =
                nonFunctionalRequirements;
    }


    public List<String> getDataRequirements() {
        return dataRequirements;
    }

    public void setDataRequirements(
            List<String> dataRequirements) {

        this.dataRequirements =
                dataRequirements;
    }


    public List<String> getSecurityRequirements() {
        return securityRequirements;
    }

    public void setSecurityRequirements(
            List<String> securityRequirements) {

        this.securityRequirements =
                securityRequirements;
    }


    public List<String> getPerformanceRequirements() {
        return performanceRequirements;
    }

    public void setPerformanceRequirements(
            List<String> performanceRequirements) {

        this.performanceRequirements =
                performanceRequirements;
    }


    public List<String> getNotificationRequirements() {
        return notificationRequirements;
    }

    public void setNotificationRequirements(
            List<String> notificationRequirements) {

        this.notificationRequirements =
                notificationRequirements;
    }


    public List<String> getErrorHandlingRequirements() {
        return errorHandlingRequirements;
    }

    public void setErrorHandlingRequirements(
            List<String> errorHandlingRequirements) {

        this.errorHandlingRequirements =
                errorHandlingRequirements;
    }


    public List<RequirementUnderstandingState.CoverageItem> getCoverage() {
        return coverage;
    }

    public void setCoverage(
            List<RequirementUnderstandingState.CoverageItem> coverage) {

        this.coverage = coverage;
    }


    public List<RequirementUnderstandingState.AmbiguityItem> getAmbiguities() {
        return ambiguities;
    }

    public void setAmbiguities(
            List<RequirementUnderstandingState.AmbiguityItem> ambiguities) {

        this.ambiguities = ambiguities;
    }


    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(
            List<String> missingInformation) {

        this.missingInformation =
                missingInformation;
    }


    public int getTotalRequirements() {
        return totalRequirements;
    }

    public void setTotalRequirements(
            int totalRequirements) {

        this.totalRequirements =
                totalRequirements;
    }


    public int getFunctionalRequirementCount() {
        return functionalRequirementCount;
    }

    public void setFunctionalRequirementCount(
            int functionalRequirementCount) {

        this.functionalRequirementCount =
                functionalRequirementCount;
    }


    public int getNonFunctionalRequirementCount() {
        return nonFunctionalRequirementCount;
    }

    public void setNonFunctionalRequirementCount(
            int nonFunctionalRequirementCount) {

        this.nonFunctionalRequirementCount =
                nonFunctionalRequirementCount;
    }


    public int getValidatedRequirementCount() {
        return validatedRequirementCount;
    }

    public void setValidatedRequirementCount(
            int validatedRequirementCount) {

        this.validatedRequirementCount =
                validatedRequirementCount;
    }


    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(
            LocalDateTime generatedAt) {

        this.generatedAt = generatedAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}