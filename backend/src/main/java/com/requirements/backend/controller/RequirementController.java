package com.requirements.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.requirements.backend.dto.RequirementCorrectionRequest;
import com.requirements.backend.dto.RequirementCreateRequest;
import com.requirements.backend.model.Interview;
import com.requirements.backend.model.Requirement;
import com.requirements.backend.service.InterviewService;
import com.requirements.backend.service.RequirementService;

@RestController
@RequestMapping("/api/requirements")
@CrossOrigin(origins = "*")
public class RequirementController {

    private final RequirementService requirementService;
    private final InterviewService interviewService;


    public RequirementController(
            RequirementService requirementService,
            InterviewService interviewService) {

        this.requirementService = requirementService;
        this.interviewService = interviewService;
    }


    // =====================================================
    // Get all requirements belonging to one interview
    // =====================================================

    @GetMapping("/interview/{interviewId}")
    public List<Requirement> getRequirementsByInterviewId(
            @PathVariable String interviewId) {

        return requirementService.getRequirementsByInterviewId(
                interviewId
        );
    }


    // =====================================================
    // Add a New Requirement During Stakeholder Review
    // =====================================================

    @PostMapping("/interview/{interviewId}")
    public Requirement createRequirement(
            @PathVariable String interviewId,
            @RequestBody RequirementCreateRequest request) {

        Interview interview =
                interviewService.getInterviewById(
                        interviewId
                );

        return requirementService.createRequirement(
                interviewId,
                interview.getProjectName(),
                request.getText(),
                request.getType()
        );
    }


    // =====================================================
    // Get one requirement using its MongoDB ID
    // =====================================================

    @GetMapping("/{id}")
    public Requirement getRequirementById(
            @PathVariable String id) {

        return requirementService.getRequirementById(id);
    }


    // =====================================================
    // Delete One Requirement
    // =====================================================

    @DeleteMapping("/{id}")
    public void deleteRequirement(
            @PathVariable String id) {

        requirementService.deleteRequirementById(id);
    }


    // =====================================================
    // Validate One Requirement
    // =====================================================

    @PatchMapping("/{id}/validate")
    public Requirement validateRequirement(
            @PathVariable String id) {

        return requirementService.updateRequirementStatus(
                id,
                "VALIDATED"
        );
    }


    // =====================================================
    // Correct and Validate One Requirement
    // =====================================================

    @PatchMapping("/{id}/correct")
    public Requirement correctRequirement(
            @PathVariable String id,
            @RequestBody RequirementCorrectionRequest request) {

        return requirementService.correctRequirement(
                id,
                request.getText(),
                request.getType()
        );
    }


    // =====================================================
    // Finalize All Requirements for One Interview
    // =====================================================

    @PatchMapping("/interview/{interviewId}/finalize")
    public List<Requirement> finalizeRequirements(
            @PathVariable String interviewId) {

        return requirementService.finalizeRequirements(
                interviewId
        );
    }
}