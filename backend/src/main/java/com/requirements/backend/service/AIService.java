package com.requirements.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.requirements.backend.dto.AnalyzeRequest;
import com.requirements.backend.dto.AnalyzeResponse;

@Service
public class AIService {

    private final RestClient restClient;

    public AIService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    public AnalyzeResponse analyzeRequirement(AnalyzeRequest request) {

        return restClient.post()
                .uri("/analyze")
                .body(request)
                .retrieve()
                .body(AnalyzeResponse.class);
    }
}