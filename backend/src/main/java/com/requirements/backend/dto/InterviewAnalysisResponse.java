package com.requirements.backend.dto;

import com.requirements.backend.model.Interview;

public class InterviewAnalysisResponse {

    private Interview interview;
    private AnalyzeResponse analysis;

    public InterviewAnalysisResponse() {
    }

    public InterviewAnalysisResponse(
            Interview interview,
            AnalyzeResponse analysis) {

        this.interview = interview;
        this.analysis = analysis;
    }

    public Interview getInterview() {
        return interview;
    }

    public void setInterview(Interview interview) {
        this.interview = interview;
    }

    public AnalyzeResponse getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AnalyzeResponse analysis) {
        this.analysis = analysis;
    }
}