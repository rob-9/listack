package com.leinterview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final GeminiService geminiService;

    public InterviewService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    private final BlockingQueue<String> transcriptionQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    private volatile boolean stopThreads = false;
    private volatile String feedbackResult = null;

    private final Random random = new Random();

    public String getLatestFeedback() {
        return feedbackResult;
    }

    @Async
    public void startInterview() {
        stopThreads = false;
        log.info("Interview service started");

        // Start the Gemini response processing thread
        Thread geminiThread = new Thread(this::processGeminiResponses);
        geminiThread.setName("gemini-processor");
        geminiThread.start();

        // Note: Audio transcription is not implemented in Java version
        // as it requires native audio libraries. This would need platform-specific implementation.
        log.info("Audio transcription not available in Java version - requires platform-specific implementation");
    }

    public void stopInterview() {
        stopThreads = true;
        log.info("Interview service stopped");
    }

    private void processGeminiResponses() {
        while (!stopThreads) {
            try {
                String transcription = transcriptionQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

                if (transcription != null) {
                    log.info("Sending to Gemini: {}", transcription);
                    feedbackResult = geminiService.getGeminiResponse(transcription);
                    responseQueue.put(feedbackResult);
                    log.info("Gemini Response: {}", feedbackResult);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Gemini processing thread interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Error processing Gemini response", e);
            }
        }
    }

    public String findProblem(String companyName) {
        try {
            ClassPathResource resource = new ClassPathResource("companies.txt");

            List<String> problems = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(companyName)) {
                        // Extract problem name after company name
                        String problem = line.substring(companyName.length()).trim();
                        if (problem.length() > 0) {
                            // Remove leading delimiter if present
                            if (problem.startsWith(":") || problem.startsWith(",")) {
                                problem = problem.substring(1).trim();
                            }
                            problems.add(problem);
                        }
                    }
                }
            }

            if (!problems.isEmpty()) {
                return problems.get(random.nextInt(problems.size()));
            }

            return "Two Sum";

        } catch (Exception e) {
            log.error("Error reading companies.txt", e);
            return "Two Sum";
        }
    }

    // Method to simulate adding transcription (for testing)
    public void addTranscription(String transcription) {
        try {
            transcriptionQueue.put(transcription);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error adding transcription", e);
        }
    }
}
