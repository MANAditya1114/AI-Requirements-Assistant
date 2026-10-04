package com.requirements.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.requirements.backend.dto.AnalyzeRequest;
import com.requirements.backend.dto.AnalyzeResponse;
import com.requirements.backend.dto.InterviewAnalysisResponse;
import com.requirements.backend.model.Interview;
import com.requirements.backend.model.User;
import com.requirements.backend.model.RequirementUnderstandingState;
import com.requirements.backend.model.StakeholderResponse;
import com.requirements.backend.repository.InterviewRepository;
import com.requirements.backend.repository.UserRepository;

@Service
public class InterviewService {

    private static final String INITIAL_QUESTION =
            "Please describe the main requirements for your system.";

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final RequirementService requirementService;

    public InterviewService(
            InterviewRepository interviewRepository,
            AIService aiService,
            RequirementService requirementService,
            UserRepository userRepository) {

        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.requirementService = requirementService;
    }

    // =====================================================
    // Create/save a new interview
    // =====================================================

    public Interview createInterview(Interview interview) {

        if (interview == null) {
            throw new IllegalStateException("Interview is required.");
        }

        if (interview.getProjectName() == null
                || interview.getProjectName().isBlank()) {
            throw new IllegalStateException("Project name is required.");
        }

        if (interview.getStakeholderName() == null
                || interview.getStakeholderName().isBlank()) {
            throw new IllegalStateException("Stakeholder name is required.");
        }

        String stakeholderUserId = interview.getStakeholderUserId();

        if (stakeholderUserId == null || stakeholderUserId.isBlank()) {
            throw new IllegalStateException("Stakeholder user ID is required.");
        }

        User stakeholder = userRepository.findById(stakeholderUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Stakeholder user not found for ID: " + stakeholderUserId));

        if (!"STAKEHOLDER".equals(stakeholder.getRole())) {
            throw new IllegalStateException(
                    "User with ID " + stakeholderUserId + " must have role STAKEHOLDER.");
        }

        String assignedBusinessAnalystId = stakeholder.getAssignedBusinessAnalystId();

        if (assignedBusinessAnalystId == null || assignedBusinessAnalystId.isBlank()) {
            throw new IllegalStateException("Stakeholder has no assigned Business Analyst.");
        }

        User assignedBusinessAnalyst = userRepository.findById(assignedBusinessAnalystId)
                .orElseThrow(() -> new IllegalStateException(
                        "Assigned Business Analyst user not found for ID: " + assignedBusinessAnalystId));

        if (!"BUSINESS_ANALYST".equals(assignedBusinessAnalyst.getRole())) {
            throw new IllegalStateException(
                    "User with ID " + assignedBusinessAnalystId
                            + " must have role BUSINESS_ANALYST.");
        }

        interview.setBusinessAnalystUserId(assignedBusinessAnalyst.getId());

        return interviewRepository.save(interview);
    }

    // =====================================================
    // Get all interviews
    // =====================================================

    public List<Interview> getAllInterviews() {

        return interviewRepository.findAll();
    }

    // =====================================================
    // Get interviews belonging to one Stakeholder
    // =====================================================

    public List<Interview> getInterviewsForStakeholder(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Stakeholder user ID is required.");
        }

        User stakeholder = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Stakeholder user not found for ID: " + userId));

        if (!"STAKEHOLDER".equals(stakeholder.getRole())) {
            throw new IllegalStateException(
                    "User with ID " + userId + " must have role STAKEHOLDER.");
        }

        return interviewRepository.findByStakeholderUserId(userId);
    }

    // =====================================================
    // Get interviews assigned to one Business Analyst
    // =====================================================

    public List<Interview> getInterviewsForBusinessAnalyst(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Business Analyst user ID is required.");
        }

        User businessAnalyst = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Business Analyst user not found for ID: " + userId));

        if (!"BUSINESS_ANALYST".equals(businessAnalyst.getRole())) {
            throw new IllegalStateException(
                    "User with ID " + userId + " must have role BUSINESS_ANALYST.");
        }

        return interviewRepository.findByBusinessAnalystUserId(userId);
    }

    // =====================================================
    // Get one interview using its MongoDB ID
    // =====================================================

    public Interview getInterviewById(String id) {

        return interviewRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Interview not found: " + id
                        )
                );
    }

    // =====================================================
    // Add stakeholder response and analyze complete interview
    // =====================================================

    public InterviewAnalysisResponse addResponse(
            String interviewId,
            String response) {

        // =================================================
        // Validate stakeholder response
        // =================================================

        if (response == null || response.isBlank()) {

            throw new IllegalArgumentException(
                    "Stakeholder response cannot be empty."
            );
        }

        // =================================================
        // Get existing interview
        // =================================================

        Interview interview =
                getInterviewById(interviewId);

        // =================================================
        // Determine the AI question that prompted this answer
        // =================================================

        String currentQuestion = INITIAL_QUESTION;

        if (interview.getUnderstandingState() != null
                && interview.getUnderstandingState().getNextQuestion() != null
                && !interview.getUnderstandingState()
                        .getNextQuestion()
                        .isBlank()) {

            currentQuestion =
                    interview.getUnderstandingState()
                            .getNextQuestion()
                            .trim();
        }

        // =================================================
        // Create new stakeholder response
        //
        // Store BOTH:
        // 1. the AI question
        // 2. the stakeholder answer
        // =================================================

        StakeholderResponse stakeholderResponse =
                new StakeholderResponse(
                        currentQuestion,
                        response.trim()
                );

        // =================================================
        // Add response IN MEMORY only
        //
        // IMPORTANT:
        // Do not save the interview yet.
        //
        // If the AI service fails, this response must not
        // remain permanently stored in MongoDB.
        // =================================================

        interview.getResponses().add(
                stakeholderResponse
        );

        // =================================================
        // Build complete multi-turn stakeholder conversation
        //
        // The AI analysis continues to use stakeholder
        // answers as before. Stored questions are for the
        // durable interview transcript.
        // =================================================

        StringBuilder conversation =
                new StringBuilder();

        int turnNumber = 1;

        for (StakeholderResponse item :
                interview.getResponses()) {

            conversation.append(
                    "Turn "
            );

            conversation.append(
                    turnNumber
            );

            conversation.append(
                    ": "
            );

            conversation.append(
                    item.getResponse()
            );

            conversation.append(
                    "\n"
            );

            turnNumber++;
        }

        // =================================================
        // Prepare request for Python AI service
        // =================================================

        AnalyzeRequest analyzeRequest =
                new AnalyzeRequest(
                        interview.getProjectName(),
                        conversation.toString()
                );

        // =================================================
        // Send complete interview to AI service
        //
        // IMPORTANT:
        // This happens BEFORE saving the new stakeholder
        // response.
        //
        // If this call throws an exception, execution stops
        // here and MongoDB keeps the previous interview.
        // =================================================

        AnalyzeResponse analysis =
                aiService.analyzeRequirement(
                        analyzeRequest
                );

        // =================================================
        // Create Requirement Understanding State
        // =================================================

        RequirementUnderstandingState understandingState =
                new RequirementUnderstandingState();

        // =================================================
        // Convert Requirements for Understanding State
        // =================================================

        List<RequirementUnderstandingState.RequirementItem>
                requirementItems =
                new ArrayList<>();

        if (analysis.getRequirements() != null) {

            for (AnalyzeResponse.RequirementResult requirement :
                    analysis.getRequirements()) {

                RequirementUnderstandingState.RequirementItem item =
                        new RequirementUnderstandingState.RequirementItem(
                                requirement.getText(),
                                requirement.getType()
                        );

                requirementItems.add(
                        item
                );
            }
        }

        understandingState.setRequirements(
                requirementItems
        );

        // =================================================
        // Convert Ambiguities
        // =================================================

        List<RequirementUnderstandingState.AmbiguityItem>
                ambiguityItems =
                new ArrayList<>();

        if (analysis.getAmbiguities() != null) {

            for (AnalyzeResponse.AmbiguityResult ambiguity :
                    analysis.getAmbiguities()) {

                RequirementUnderstandingState.AmbiguityItem item =
                        new RequirementUnderstandingState.AmbiguityItem(
                                ambiguity.getText(),
                                ambiguity.getReason()
                        );

                ambiguityItems.add(
                        item
                );
            }
        }

        understandingState.setAmbiguities(
                ambiguityItems
        );

        // =================================================
        // Store Missing Information
        // =================================================

        if (analysis.getMissingInformation() != null) {

            understandingState.setMissingInformation(
                    new ArrayList<>(
                            analysis.getMissingInformation()
                    )
            );

        } else {

            understandingState.setMissingInformation(
                    new ArrayList<>()
            );
        }

        // =================================================
        // Convert Coverage
        // =================================================

        List<RequirementUnderstandingState.CoverageItem>
                coverageItems =
                new ArrayList<>();

        if (analysis.getCoverage() != null) {

            for (AnalyzeResponse.CoverageResult coverage :
                    analysis.getCoverage()) {

                RequirementUnderstandingState.CoverageItem item =
                        new RequirementUnderstandingState.CoverageItem(
                                coverage.getArea(),
                                coverage.getStatus()
                        );

                coverageItems.add(
                        item
                );
            }
        }

        understandingState.setCoverage(
                coverageItems
        );

        // =================================================
        // Store Adaptive Follow-Up Question
        // =================================================

        understandingState.setNextQuestion(
                analysis.getNextQuestion()
        );

        // =================================================
        // Update Understanding State Timestamp
        // =================================================

        understandingState.setUpdatedAt(
                LocalDateTime.now()
        );

        // =================================================
        // Attach Current State to Interview
        // =================================================

        interview.setUnderstandingState(
                understandingState
        );

        // =================================================
        // Save Interview ONLY AFTER Successful AI Analysis
        // =================================================

        interview =
                interviewRepository.save(
                        interview
                );

        // =================================================
        // Synchronize Persistent Requirements
        // =================================================

        requirementService.synchronizeRequirements(
                interview.getId(),
                interview.getProjectName(),
                analysis.getRequirements()
        );

        // =================================================
        // Return Interview + Current AI Analysis
        // =================================================

        return new InterviewAnalysisResponse(
                interview,
                analysis
        );
    }
}