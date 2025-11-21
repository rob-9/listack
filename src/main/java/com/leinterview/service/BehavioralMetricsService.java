package com.leinterview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BehavioralMetricsService {

    private static final Logger log = LoggerFactory.getLogger(BehavioralMetricsService.class);

    @Autowired
    private VideoProcessingService videoProcessingService;

    /**
     * Analyze interview performance based on video behavioral metrics
     */
    public InterviewAnalysis analyzeInterview(String videoPath) {
        log.info("Analyzing interview video: {}", videoPath);

        Map<String, Object> videoMetrics = videoProcessingService.processInterviewVideo(videoPath);
        InterviewAnalysis analysis = new InterviewAnalysis();

        // Extract metrics
        double faceDetectionRate = (double) videoMetrics.getOrDefault("faceDetectionRate", 0.0);
        double eyeContactRate = (double) videoMetrics.getOrDefault("eyeContactRate", 0.0);
        double averageConfidence = (double) videoMetrics.getOrDefault("averageConfidence", 0.0);
        double duration = (double) videoMetrics.getOrDefault("duration", 0.0);

        // Calculate engagement score (0-100)
        double engagementScore = calculateEngagementScore(faceDetectionRate, eyeContactRate, averageConfidence);
        analysis.setEngagementScore(engagementScore);

        // Generate feedback
        analysis.setFaceDetectionRate(faceDetectionRate * 100);
        analysis.setEyeContactRate(eyeContactRate * 100);
        analysis.setDuration(duration);
        analysis.setFeedback(generateFeedback(faceDetectionRate, eyeContactRate, averageConfidence));
        analysis.setRawMetrics(videoMetrics);

        // Overall assessment
        if (engagementScore >= 80) {
            analysis.setAssessment("Excellent");
        } else if (engagementScore >= 60) {
            analysis.setAssessment("Good");
        } else if (engagementScore >= 40) {
            analysis.setAssessment("Fair");
        } else {
            analysis.setAssessment("Needs Improvement");
        }

        log.info("Interview analysis complete. Engagement score: {}", engagementScore);
        return analysis;
    }

    /**
     * Calculate overall engagement score based on multiple factors
     */
    private double calculateEngagementScore(double faceDetectionRate, double eyeContactRate, double confidence) {
        // Weighted scoring
        double faceWeight = 0.3;
        double eyeContactWeight = 0.5;
        double confidenceWeight = 0.2;

        return (faceDetectionRate * faceWeight +
                eyeContactRate * eyeContactWeight +
                confidence * confidenceWeight) * 100;
    }

    /**
     * Generate personalized feedback based on metrics
     */
    private String generateFeedback(double faceDetectionRate, double eyeContactRate, double confidence) {
        StringBuilder feedback = new StringBuilder();

        // Face detection feedback
        if (faceDetectionRate < 0.7) {
            feedback.append("Consider positioning yourself more consistently in the camera frame. ");
        } else if (faceDetectionRate >= 0.9) {
            feedback.append("Excellent camera positioning throughout the interview. ");
        }

        // Eye contact feedback
        if (eyeContactRate < 0.5) {
            feedback.append("Try to maintain more eye contact with the camera to show engagement. ");
        } else if (eyeContactRate >= 0.7) {
            feedback.append("Great eye contact - this shows confidence and engagement. ");
        }

        // Confidence feedback
        if (confidence < 0.6) {
            feedback.append("Work on appearing more confident and composed during interviews. ");
        } else if (confidence >= 0.8) {
            feedback.append("You displayed strong confidence and composure. ");
        }

        if (feedback.length() == 0) {
            feedback.append("Good overall performance with room for minor improvements.");
        }

        return feedback.toString().trim();
    }

    /**
     * Compare two interview analyses to track improvement
     */
    public Map<String, Object> compareAnalyses(InterviewAnalysis previous, InterviewAnalysis current) {
        Map<String, Object> comparison = new HashMap<>();

        double engagementImprovement = current.getEngagementScore() - previous.getEngagementScore();
        double faceDetectionImprovement = current.getFaceDetectionRate() - previous.getFaceDetectionRate();
        double eyeContactImprovement = current.getEyeContactRate() - previous.getEyeContactRate();

        comparison.put("engagementImprovement", engagementImprovement);
        comparison.put("faceDetectionImprovement", faceDetectionImprovement);
        comparison.put("eyeContactImprovement", eyeContactImprovement);
        comparison.put("trending", engagementImprovement > 0 ? "improving" : engagementImprovement < 0 ? "declining" : "stable");

        return comparison;
    }

    public static class InterviewAnalysis {
        private double engagementScore;
        private double faceDetectionRate;
        private double eyeContactRate;
        private double duration;
        private String assessment;
        private String feedback;
        private Map<String, Object> rawMetrics;

        public double getEngagementScore() {
            return engagementScore;
        }

        public void setEngagementScore(double engagementScore) {
            this.engagementScore = engagementScore;
        }

        public double getFaceDetectionRate() {
            return faceDetectionRate;
        }

        public void setFaceDetectionRate(double faceDetectionRate) {
            this.faceDetectionRate = faceDetectionRate;
        }

        public double getEyeContactRate() {
            return eyeContactRate;
        }

        public void setEyeContactRate(double eyeContactRate) {
            this.eyeContactRate = eyeContactRate;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public String getAssessment() {
            return assessment;
        }

        public void setAssessment(String assessment) {
            this.assessment = assessment;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public Map<String, Object> getRawMetrics() {
            return rawMetrics;
        }

        public void setRawMetrics(Map<String, Object> rawMetrics) {
            this.rawMetrics = rawMetrics;
        }
    }
}
