package com.requirements.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyzeResponse {

    private List<RequirementResult> requirements;

    private List<AmbiguityResult> ambiguities;

    @JsonProperty("missing_information")
    private List<String> missingInformation;

    private List<CoverageResult> coverage;

    @JsonProperty("next_question")
    private String nextQuestion;


    public AnalyzeResponse() {
    }


    // =====================================================
    // Requirements
    // =====================================================

    public List<RequirementResult> getRequirements() {
        return requirements;
    }

    public void setRequirements(
            List<RequirementResult> requirements) {

        this.requirements = requirements;
    }


    // =====================================================
    // Ambiguities
    // =====================================================

    public List<AmbiguityResult> getAmbiguities() {
        return ambiguities;
    }

    public void setAmbiguities(
            List<AmbiguityResult> ambiguities) {

        this.ambiguities = ambiguities;
    }


    // =====================================================
    // Missing Information
    // =====================================================

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(
            List<String> missingInformation) {

        this.missingInformation = missingInformation;
    }


    // =====================================================
    // Coverage
    // =====================================================

    public List<CoverageResult> getCoverage() {
        return coverage;
    }

    public void setCoverage(
            List<CoverageResult> coverage) {

        this.coverage = coverage;
    }


    // =====================================================
    // Next Question
    // =====================================================

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }


    // =====================================================
    // Requirement Result
    // =====================================================

    public static class RequirementResult {

        private String text;
        private String type;


        public RequirementResult() {
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
    // Ambiguity Result
    // =====================================================

    public static class AmbiguityResult {

        private String text;
        private String reason;


        public AmbiguityResult() {
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
    // Coverage Result
    // =====================================================

    public static class CoverageResult {

        private String area;
        private String status;


        public CoverageResult() {
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