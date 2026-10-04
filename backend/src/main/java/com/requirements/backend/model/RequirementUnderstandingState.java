package com.requirements.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RequirementUnderstandingState {

    private List<RequirementItem> requirements = new ArrayList<>();
    private List<AmbiguityItem> ambiguities = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<CoverageItem> coverage = new ArrayList<>();
    private String nextQuestion;
    private LocalDateTime updatedAt;

    public RequirementUnderstandingState() {
        this.updatedAt = LocalDateTime.now();
    }

    public List<RequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<RequirementItem> requirements) {
        this.requirements = requirements;
    }

    public List<AmbiguityItem> getAmbiguities() {
        return ambiguities;
    }

    public void setAmbiguities(List<AmbiguityItem> ambiguities) {
        this.ambiguities = ambiguities;
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = missingInformation;
    }

    public List<CoverageItem> getCoverage() {
        return coverage;
    }

    public void setCoverage(List<CoverageItem> coverage) {
        this.coverage = coverage;
    }

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    // =====================================================
    // Requirement Item
    // =====================================================

    public static class RequirementItem {

        private String text;
        private String type;

        public RequirementItem() {
        }

        public RequirementItem(String text, String type) {
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }


    // =====================================================
    // Ambiguity Item
    // =====================================================

    public static class AmbiguityItem {

        private String text;
        private String reason;

        public AmbiguityItem() {
        }

        public AmbiguityItem(String text, String reason) {
            this.text = text;
            this.reason = reason;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }


    // =====================================================
    // Coverage Item
    // =====================================================

    public static class CoverageItem {

        private String area;
        private String status;

        public CoverageItem() {
        }

        public CoverageItem(String area, String status) {
            this.area = area;
            this.status = status;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}