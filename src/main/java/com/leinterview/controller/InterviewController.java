package com.leinterview.controller;

import com.leinterview.service.CodeExecutionService;
import com.leinterview.service.GeminiService;
import com.leinterview.service.InterviewService;
import com.leinterview.service.KubernetesCodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;
    private final CodeExecutionService codeExecutionService;
    private final GeminiService geminiService;
    private final KubernetesCodeExecutor kubernetesExecutor;

    public InterviewController(InterviewService interviewService,
                              CodeExecutionService codeExecutionService,
                              GeminiService geminiService,
                              KubernetesCodeExecutor kubernetesExecutor) {
        this.interviewService = interviewService;
        this.codeExecutionService = codeExecutionService;
        this.geminiService = geminiService;
        this.kubernetesExecutor = kubernetesExecutor;
    }

    @Value("${interview.use-kubernetes:false}")
    private boolean useKubernetes;

    @Value("${interview.audio-enabled:false}")
    private boolean audioEnabled;

    @Value("${interview.demo-mode:true}")
    private boolean demoMode;

    @GetMapping("/get-feedback")
    public ResponseEntity<Map<String, String>> getFeedback() {
        String feedback = interviewService.getLatestFeedback();
        Map<String, String> response = new HashMap<>();
        response.put("feedback", feedback);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start-interview")
    public ResponseEntity<Map<String, String>> startInterview() {
        log.info("Starting interview...");
        interviewService.startInterview();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Interview started");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop-interview")
    public ResponseEntity<Map<String, String>> stopInterview() {
        log.info("Stopping interview...");
        interviewService.stopInterview();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Interview stopped");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.getOrDefault("language", "python").toLowerCase();

        log.info("Executing {} code", language);

        String output;
        if (useKubernetes) {
            try {
                Map<String, String> result = kubernetesExecutor.executeCode(code, language, 30);
                output = "succeeded".equals(result.get("status")) ?
                        result.get("output") : result.get("error");
            } catch (Exception e) {
                log.error("Kubernetes execution failed, falling back to subprocess", e);
                output = codeExecutionService.executeCode(code, language);
            }
        } else {
            output = codeExecutionService.executeCode(code, language);
        }

        Map<String, String> response = new HashMap<>();
        response.put("output", output);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/random-quest/{companyName}")
    public ResponseEntity<Map<String, String>> getRandomQuestion(@PathVariable String companyName) {
        log.info("Getting random question for company: {}", companyName);

        String problem = interviewService.findProblem(companyName);
        String question = geminiService.getResponseFromGemini(problem);

        Map<String, String> response = new HashMap<>();
        response.put("return_question", question);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "healthy");
        status.put("use_kubernetes", useKubernetes);
        status.put("audio_enabled", audioEnabled);
        status.put("genai_available", !demoMode);

        if (useKubernetes) {
            try {
                int activeJobs = kubernetesExecutor.getActiveJobs();
                status.put("active_jobs", activeJobs);
            } catch (Exception e) {
                status.put("k8s_error", e.getMessage());
            }
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("use_kubernetes", useKubernetes);
        metrics.put("demo_mode", demoMode);

        if (useKubernetes) {
            try {
                metrics.put("active_jobs", kubernetesExecutor.getActiveJobs());
            } catch (Exception e) {
                metrics.put("error", e.getMessage());
            }
        }

        return ResponseEntity.ok(metrics);
    }
}
