package com.requirements.backend.model;

import java.time.LocalDateTime;

public class StakeholderResponse {

    private String question;
    private String response;
    private LocalDateTime timestamp;

    public StakeholderResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public StakeholderResponse(String response) {
        this.response = response;
        this.timestamp = LocalDateTime.now();
    }

    public StakeholderResponse(String question, String response) {
        this.question = question;
        this.response = response;
        this.timestamp = LocalDateTime.now();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}