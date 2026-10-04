package com.requirements.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.requirements.backend.model.SRS;
import com.requirements.backend.service.SRSService;

@RestController
@RequestMapping("/api/srs")
@CrossOrigin(origins = "*")
public class SRSController {

    private final SRSService srsService;


    public SRSController(
            SRSService srsService) {

        this.srsService = srsService;
    }


    // =====================================================
    // Generate SRS from Validated Requirements
    // =====================================================

    @PostMapping("/interview/{interviewId}/generate")
    public SRS generateSRS(
            @PathVariable String interviewId) {

        return srsService.generateSRS(
                interviewId
        );
    }


    // =====================================================
    // Get Existing SRS for an Interview
    // =====================================================

    @GetMapping("/interview/{interviewId}")
    public SRS getSRSByInterviewId(
            @PathVariable String interviewId) {

        return srsService.getSRSByInterviewId(
                interviewId
        );
    }


    // =====================================================
    // Business Analyst Reviews SRS
    // =====================================================

    @PatchMapping("/interview/{interviewId}/review")
    public SRS reviewSRS(
            @PathVariable String interviewId) {

        return srsService.reviewSRS(
                interviewId
        );
    }
}