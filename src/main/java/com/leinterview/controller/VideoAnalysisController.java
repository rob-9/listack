package com.leinterview.controller;

import com.leinterview.service.BehavioralMetricsService;
import com.leinterview.service.VideoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(VideoAnalysisController.class);

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private BehavioralMetricsService behavioralMetricsService;

    private final String uploadDir = System.getProperty("java.io.tmpdir") + "/interview-videos/";

    public VideoAnalysisController() {
        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
        }
    }

    /**
     * Upload and analyze an interview video
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        try {
            // Save uploaded file
            String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);
            file.transferTo(filepath.toFile());

            log.info("Processing uploaded video: {}", filename);

            // Analyze the video
            BehavioralMetricsService.InterviewAnalysis analysis =
                    behavioralMetricsService.analyzeInterview(filepath.toString());

            Map<String, Object> response = new HashMap<>();
            response.put("analysis", analysis);
            response.put("videoId", filename);
            response.put("status", "success");

            // Clean up the file after processing
            Files.delete(filepath);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get video metadata without full analysis
     */
    @PostMapping(value = "/metadata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoMetadata(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        try {
            String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);
            file.transferTo(filepath.toFile());

            Map<String, Object> metadata = videoProcessingService.getVideoMetadata(filepath.toString());

            Files.delete(filepath);

            return ResponseEntity.ok(metadata);

        } catch (Exception e) {
            log.error("Error reading video metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Extract a thumbnail from a video
     */
    @PostMapping(value = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> extractThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "timestamp", defaultValue = "0.0") double timestamp) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);
            file.transferTo(filepath.toFile());

            BufferedImage thumbnail = videoProcessingService.extractThumbnail(filepath.toString(), timestamp);

            Files.delete(filepath);

            if (thumbnail == null) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("Error extracting thumbnail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "video-analysis"
        ));
    }
}
