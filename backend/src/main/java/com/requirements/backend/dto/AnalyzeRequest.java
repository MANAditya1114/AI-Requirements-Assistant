package com.requirements.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyzeRequest {

    @JsonProperty("project_name")
    private String projectName;

    private String response;

    public AnalyzeRequest() {
    }

    public AnalyzeRequest(String projectName, String response) {
        this.projectName = projectName;
        this.response = response;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}