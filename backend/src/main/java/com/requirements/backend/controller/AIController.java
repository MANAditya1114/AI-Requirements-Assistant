package com.requirements.backend.controller;

import org.springframework.web.bind.annotation.*;

import com.requirements.backend.dto.AnalyzeRequest;
import com.requirements.backend.dto.AnalyzeResponse;
import com.requirements.backend.service.AIService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyzeRequirement(
            @RequestBody AnalyzeRequest request) {

        return aiService.analyzeRequirement(request);
    }
}