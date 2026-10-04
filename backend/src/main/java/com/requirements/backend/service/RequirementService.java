package com.requirements.backend.service;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.List;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.requirements.backend.dto.AnalyzeResponse;

import com.requirements.backend.model.Requirement;

import com.requirements.backend.repository.RequirementRepository;

@Service

public class RequirementService {

    private final RequirementRepository requirementRepository;

    public RequirementService(

            RequirementRepository requirementRepository) {

        this.requirementRepository = requirementRepository;

    }

    // =====================================================

    // Manually Add Requirement During Stakeholder Review

    // =====================================================

    public Requirement createRequirement(

            String interviewId,

            String projectName,

            String text,

            String type) {

        if (interviewId == null || interviewId.isBlank()) {

            throw new IllegalArgumentException(

                    "Interview ID cannot be empty."

            );

        }

        if (projectName == null || projectName.isBlank()) {

            throw new IllegalArgumentException(

                    "Project name cannot be empty."

            );

        }

        if (text == null || text.isBlank()) {

            throw new IllegalArgumentException(

                    "Requirement text cannot be empty."

            );

        }

        if (type == null || type.isBlank()) {

            throw new IllegalArgumentException(

                    "Requirement type cannot be empty."

            );

        }

        String normalizedType =

                type.trim().toUpperCase(java.util.Locale.ROOT);

        if (!normalizedType.equals("FUNCTIONAL")

                && !normalizedType.equals("NON_FUNCTIONAL")) {

            throw new IllegalArgumentException(

                    "Requirement type must be FUNCTIONAL or NON_FUNCTIONAL."

            );

        }

        String trimmedText = text.trim();

        Requirement requirement = new Requirement(

                interviewId,

                projectName,

                trimmedText,

                normalizedType

        );

        requirement.setOriginalText(trimmedText);

        requirement.setStakeholderModified(true);

        requirement.setStatus("DISCOVERED");

        LocalDateTime createdAt = LocalDateTime.now();

        requirement.setCreatedAt(createdAt);

        requirement.setUpdatedAt(createdAt);

        return requirementRepository.save(requirement);

    }

    // =====================================================

    // Save Requirement

    // =====================================================

    public Requirement saveRequirement(

            Requirement requirement) {

        if (requirement.getOriginalText() == null

                || requirement.getOriginalText().isBlank()) {

            requirement.setOriginalText(

                    requirement.getText()

            );

        }

        requirement.setUpdatedAt(

                LocalDateTime.now()

        );

        return requirementRepository.save(

                requirement

        );

    }

    // =====================================================

    // Get Requirements for One Interview

    // =====================================================

    public List<Requirement> getRequirementsByInterviewId(

            String interviewId) {

        return requirementRepository.findByInterviewId(

                interviewId

        );

    }

    // =====================================================

    // Get Requirement by ID

    // =====================================================

    public Requirement getRequirementById(

            String id) {

        return requirementRepository.findById(id)

                .orElseThrow(() ->

                        new RuntimeException(

                                "Requirement not found: " + id

                        )

                );

    }

    public void deleteRequirementById(String id) {
        Requirement requirement = getRequirementById(id);
        requirementRepository.delete(requirement);
    }

    // =====================================================

    // Update Requirement Status

    // =====================================================

    public Requirement updateRequirementStatus(

            String id,

            String status) {

        Requirement requirement =

                getRequirementById(id);

        if (requirement.getOriginalText() == null

                || requirement.getOriginalText().isBlank()) {

            requirement.setOriginalText(

                    requirement.getText()

            );

        }

        requirement.setStatus(status);

        requirement.setUpdatedAt(

                LocalDateTime.now()

        );

        return requirementRepository.save(

                requirement

        );

    }

    // =====================================================

    // Correct Requirement

    // =====================================================

    public Requirement correctRequirement(

            String id,

            String correctedText,

            String correctedType) {

        Requirement requirement =

                getRequirementById(id);

        if (correctedText == null

                || correctedText.isBlank()) {

            throw new IllegalArgumentException(

                    "Corrected requirement text cannot be empty."

            );

        }

        if (correctedType == null

                || correctedType.isBlank()) {

            throw new IllegalArgumentException(

                    "Requirement type cannot be empty."

            );

        }

        String normalizedType =

                correctedType.trim().toUpperCase();

        if (!normalizedType.equals("FUNCTIONAL")

                && !normalizedType.equals(

                        "NON_FUNCTIONAL"

                )) {

            throw new IllegalArgumentException(

                    "Requirement type must be FUNCTIONAL or NON_FUNCTIONAL."

            );

        }

        /*

         * Preserve the AI wording before applying

         * the stakeholder correction.

         */

        if (requirement.getOriginalText() == null

                || requirement.getOriginalText().isBlank()) {

            requirement.setOriginalText(

                    requirement.getText()

            );

        }

        requirement.setText(

                correctedText.trim()

        );

        requirement.setType(

                normalizedType

        );

        requirement.setStatus(

                "VALIDATED"

        );

        requirement.setStakeholderModified(

                true

        );

        requirement.setUpdatedAt(

                LocalDateTime.now()

        );

        return requirementRepository.save(

                requirement

        );

    }

    // =====================================================

    // Finalize All Requirements for One Interview

    // =====================================================

    public List<Requirement> finalizeRequirements(

            String interviewId) {

        List<Requirement> requirements =

                requirementRepository.findByInterviewId(

                        interviewId

                );

        if (requirements.isEmpty()) {

            throw new IllegalStateException(

                    "No requirements found for interview: "

                            + interviewId

            );

        }

        LocalDateTime finalizedAt =

                LocalDateTime.now();

        for (Requirement requirement : requirements) {

            if (requirement.getOriginalText() == null

                    || requirement.getOriginalText().isBlank()) {

                requirement.setOriginalText(

                        requirement.getText()

                );

            }

            requirement.setStatus(

                    "VALIDATED"

            );

            requirement.setUpdatedAt(

                    finalizedAt

            );

        }

        return requirementRepository.saveAll(

                requirements

        );

    }

    // =====================================================

    // Delete Requirements for Interview

    // =====================================================

    public void deleteRequirementsByInterviewId(

            String interviewId) {

        List<Requirement> requirements =

                requirementRepository.findByInterviewId(

                        interviewId

                );

        requirementRepository.deleteAll(

                requirements

        );

    }

    // =====================================================

    // Synchronize Latest AI Requirements

    // =====================================================

    public List<Requirement> synchronizeRequirements(

            String interviewId,

            String projectName,

            List<AnalyzeResponse.RequirementResult>

                    latestRequirements) {

        List<Requirement> existingRequirements =

                requirementRepository.findByInterviewId(

                        interviewId

                );

        // =================================================

        // Initialize originalText for old normal records

        // =================================================

        for (Requirement existing :

                existingRequirements) {

            if ((existing.getOriginalText() == null

                    || existing.getOriginalText().isBlank())

                    && !existing.isStakeholderModified()) {

                existing.setOriginalText(

                        existing.getText()

                );

                requirementRepository.save(

                        existing

                );

            }

        }

        List<Requirement> synchronizedRequirements =

                new ArrayList<>();

        if (latestRequirements == null) {

            return existingRequirements;

        }

        // =================================================

        // Process Latest AI Requirements

        // =================================================

        for (AnalyzeResponse.RequirementResult latest :

                latestRequirements) {

            if (latest.getText() == null

                    || latest.getText().isBlank()) {

                continue;

            }

            if (latest.getType() == null

                    || latest.getType().isBlank()) {

                continue;

            }

            String latestText =

                    latest.getText().trim();

            Requirement matchingRequirement =

                    null;

            // =============================================

            // STEP 1:

            // Match against current requirement text

            // =============================================

            for (Requirement existing :

                    existingRequirements) {

                if (alreadySynchronized(

                        synchronizedRequirements,

                        existing

                )) {

                    continue;

                }

                if (sameText(

                        existing.getText(),

                        latestText

                )) {

                    matchingRequirement =

                            existing;

                    break;

                }

            }

            // =============================================

            // STEP 2:

            // Match against original AI wording

            // =============================================

            if (matchingRequirement == null) {

                for (Requirement existing :

                        existingRequirements) {

                    if (alreadySynchronized(

                            synchronizedRequirements,

                            existing

                    )) {

                        continue;

                    }

                    if (sameText(

                            existing.getOriginalText(),

                            latestText

                    )) {

                        matchingRequirement =

                                existing;

                        break;

                    }

                }

            }

            // =============================================

            // STEP 3:

            // Conservative Semantic Refinement Match

            // =============================================

            if (matchingRequirement == null) {

                matchingRequirement =

                        findUniqueSemanticMatch(

                                existingRequirements,

                                synchronizedRequirements,

                                latestText

                        );

            }

            // =============================================

            // Existing Requirement Found

            // =============================================

            if (matchingRequirement != null) {

                if (matchingRequirement.getOriginalText()

                        == null

                        || matchingRequirement

                                .getOriginalText()

                                .isBlank()) {

                    matchingRequirement.setOriginalText(

                            latestText

                    );

                }

                /*

                 * Stakeholder-modified requirements are

                 * authoritative.

                 *

                 * AI is not allowed to overwrite their

                 * corrected text, type, or status.

                 */

                if (!matchingRequirement

                        .isStakeholderModified()) {

                    matchingRequirement.setText(

                            latestText

                    );

                    matchingRequirement.setType(

                            latest.getType()

                    );

                }

                matchingRequirement.setUpdatedAt(

                        LocalDateTime.now()

                );

                Requirement saved =

                        requirementRepository.save(

                                matchingRequirement

                        );

                addIfMissingById(

                        synchronizedRequirements,

                        saved

                );

            }

            // =============================================

            // New Requirement

            // =============================================

            else {

                Requirement newRequirement =

                        new Requirement(

                                interviewId,

                                projectName,

                                latestText,

                                latest.getType()

                        );

                newRequirement.setOriginalText(

                        latestText

                );

                Requirement saved =

                        requirementRepository.save(

                                newRequirement

                        );

                addIfMissingById(

                        synchronizedRequirements,

                        saved

                );

            }

        }

        // =================================================

        // Preserve Stakeholder-Modified Requirements

        // =================================================

        for (Requirement existing :

                existingRequirements) {

            if (existing.isStakeholderModified()) {

                addIfMissingById(

                        synchronizedRequirements,

                        existing

                );

            }

        }

        // =================================================

        // Preserve Validated Requirements

        // =================================================

        for (Requirement existing :

                existingRequirements) {

            if ("VALIDATED".equalsIgnoreCase(

                    existing.getStatus()

            )) {

                addIfMissingById(

                        synchronizedRequirements,

                        existing

                );

            }

        }

        // =================================================

        // Remove Obsolete AI Requirements

        // =================================================

        for (Requirement existing :

                existingRequirements) {

            boolean stillExists =

                    containsRequirementId(

                            synchronizedRequirements,

                            existing.getId()

                    );

            if (!stillExists

                    && !"VALIDATED".equalsIgnoreCase(

                            existing.getStatus()

                    )

                    && !existing.isStakeholderModified()) {

                requirementRepository.delete(

                        existing

                );

            }

        }

        return synchronizedRequirements;

    }

    // =====================================================

    // Find One Safe Semantic Refinement Match

    // =====================================================

    private Requirement findUniqueSemanticMatch(

            List<Requirement> existingRequirements,

            List<Requirement> synchronizedRequirements,

            String latestText) {

        Requirement bestMatch = null;

        double bestScore = 0.0;

        double secondBestScore = 0.0;

        for (Requirement existing :

                existingRequirements) {

            if (alreadySynchronized(

                    synchronizedRequirements,

                    existing

            )) {

                continue;

            }

            double currentTextScore =

                    semanticSimilarity(

                            existing.getText(),

                            latestText

                    );

            double originalTextScore =

                    semanticSimilarity(

                            existing.getOriginalText(),

                            latestText

                    );

            double score =

                    Math.max(

                            currentTextScore,

                            originalTextScore

                    );

            if (score > bestScore) {

                secondBestScore = bestScore;

                bestScore = score;

                bestMatch = existing;

            } else if (score > secondBestScore) {

                secondBestScore = score;

            }

        }

        /*

         * Require a strong similarity.

         *

         * Also require a clear difference between the best

         * and second-best candidate. This reduces accidental

         * merging when two requirements discuss the same

         * feature but represent different actions.

         */

        if (bestMatch != null

                && bestScore >= 0.72

                && (bestScore - secondBestScore) >= 0.10) {

            return bestMatch;

        }

        return null;

    }

    // =====================================================

    // Conservative Semantic Similarity

    // =====================================================

    private double semanticSimilarity(

            String first,

            String second) {

        if (first == null

                || second == null) {

            return 0.0;

        }

        String normalizedFirst =

                normalizeText(first);

        String normalizedSecond =

                normalizeText(second);

        if (normalizedFirst.isBlank()

                || normalizedSecond.isBlank()) {

            return 0.0;

        }

        if (normalizedFirst.equals(

                normalizedSecond

        )) {

            return 1.0;

        }

        Set<String> firstWords =

                meaningfulWords(

                        normalizedFirst

                );

        Set<String> secondWords =

                meaningfulWords(

                        normalizedSecond

                );

        if (firstWords.isEmpty()

                || secondWords.isEmpty()) {

            return 0.0;

        }

        Set<String> intersection =

                new HashSet<>(

                        firstWords

                );

        intersection.retainAll(

                secondWords

        );

        double smallerCoverage =

                (double) intersection.size()

                        / Math.min(

                                firstWords.size(),

                                secondWords.size()

                        );

        Set<String> union =

                new HashSet<>(

                        firstWords

                );

        union.addAll(

                secondWords

        );

        double jaccard =

                (double) intersection.size()

                        / union.size();

        return (

                smallerCoverage * 0.65

                + jaccard * 0.35

        );

    }

    // =====================================================

    // Extract Meaningful Words

    // =====================================================

    private Set<String> meaningfulWords(

            String normalizedText) {

        Set<String> words =

                new HashSet<>();

        Set<String> ignoredWords =

                Set.of(

                        "a",

                        "an",

                        "the",

                        "and",

                        "or",

                        "to",

                        "of",

                        "in",

                        "on",

                        "for",

                        "with",

                        "using",

                        "be",

                        "is",

                        "are",

                        "was",

                        "were",

                        "will",

                        "would",

                        "should",

                        "could",

                        "can",

                        "must",

                        "system"

                );

        String[] parts =

                normalizedText.split("\\s+");

        for (String part : parts) {

            if (part.isBlank()) {

                continue;

            }

            if (ignoredWords.contains(part)) {

                continue;

            }

            words.add(part);

        }

        return words;

    }

    // =====================================================

    // Check Whether Requirement Was Already Matched

    // =====================================================

    private boolean alreadySynchronized(

            List<Requirement> synchronizedRequirements,

            Requirement requirement) {

        if (requirement == null

                || requirement.getId() == null) {

            return false;

        }

        return containsRequirementId(

                synchronizedRequirements,

                requirement.getId()

        );

    }

    // =====================================================

    // Normalize Requirement Text

    // =====================================================

    private String normalizeText(

            String text) {

        if (text == null) {

            return "";

        }

        String normalized =

                text.trim()

                        .toLowerCase();

        normalized =

                normalized.replaceAll(

                        "[^a-z0-9]+",

                        " "

                );

        normalized =

                normalized.replaceAll(

                        "\\s+",

                        " "

                );

        return normalized.trim();

    }

    // =====================================================

    // Compare Requirement Text

    // =====================================================

    private boolean sameText(

            String first,

            String second) {

        if (first == null

                || second == null) {

            return false;

        }

        String normalizedFirst =

                normalizeText(first);

        String normalizedSecond =

                normalizeText(second);

        if (normalizedFirst.isBlank()

                || normalizedSecond.isBlank()) {

            return false;

        }

        return normalizedFirst.equals(

                normalizedSecond

        );

    }

    // =====================================================

    // Check Whether List Contains Requirement ID

    // =====================================================

    private boolean containsRequirementId(

            List<Requirement> requirements,

            String requirementId) {

        if (requirementId == null) {

            return false;

        }

        for (Requirement requirement :

                requirements) {

            if (requirementId.equals(

                    requirement.getId()

            )) {

                return true;

            }

        }

        return false;

    }

    // =====================================================

    // Add Requirement Only Once

    // =====================================================

    private void addIfMissingById(

            List<Requirement> requirements,

            Requirement requirement) {

        if (requirement == null) {

            return;

        }

        if (!containsRequirementId(

                requirements,

                requirement.getId()

        )) {

            requirements.add(

                    requirement

            );

        }

    }

}
