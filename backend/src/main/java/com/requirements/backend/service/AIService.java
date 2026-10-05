package com.requirements.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.requirements.backend.dto.AnalyzeRequest;
import com.requirements.backend.dto.AnalyzeResponse;

@Service
public class AIService {

    private final RestClient restClient;

    public AIService(
            @Value("${ai.service.url:http://127.0.0.1:8000}")
            String aiServiceUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
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