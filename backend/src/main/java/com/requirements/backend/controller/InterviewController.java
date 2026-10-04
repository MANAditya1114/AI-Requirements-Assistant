package com.requirements.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.requirements.backend.dto.InterviewAnalysisResponse;
import com.requirements.backend.dto.InterviewResponse;
import com.requirements.backend.model.Interview;
import com.requirements.backend.service.InterviewService;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // =====================================================
    // Create a new interview
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Interview createInterview(
            @RequestBody Interview interview) {

        return interviewService.createInterview(interview);
    }

    // =====================================================
    // Get all interviews
    // Kept temporarily for development/testing
    // =====================================================

    @GetMapping
    public List<Interview> getAllInterviews() {

        return interviewService.getAllInterviews();
    }

    // =====================================================
    // Get interviews belonging to one Stakeholder
    // =====================================================

    @GetMapping("/stakeholder/{userId}")
    public List<Interview> getInterviewsForStakeholder(
            @PathVariable String userId) {

        return interviewService
                .getInterviewsForStakeholder(userId);
    }

    // =====================================================
    // Get interviews assigned to one Business Analyst
    // =====================================================

    @GetMapping("/business-analyst/{userId}")
    public List<Interview> getInterviewsForBusinessAnalyst(
            @PathVariable String userId) {

        return interviewService
                .getInterviewsForBusinessAnalyst(userId);
    }

    // =====================================================
    // Get one interview using its MongoDB ID
    // =====================================================

    @GetMapping("/{id}")
    public Interview getInterviewById(
            @PathVariable String id) {

        return interviewService.getInterviewById(id);
    }

    // =====================================================
    // Add stakeholder response and run AI analysis
    // =====================================================

    @PostMapping("/{id}/respond")
    public InterviewAnalysisResponse addResponse(
            @PathVariable String id,
            @RequestBody InterviewResponse request) {

        return interviewService.addResponse(
                id,
                request.getResponse()
        );
    }
}