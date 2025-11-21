package com.leinterview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;
    private Java2DFrameConverter converter;

    @PostConstruct
    public void init() {
        try {
            converter = new Java2DFrameConverter();

            // Load Haar Cascade classifiers for face and eye detection
            // These are bundled with OpenCV
            String faceCascadePath = getClass().getClassLoader()
                .getResource("haarcascade_frontalface_default.xml")
                .getPath();
            String eyeCascadePath = getClass().getClassLoader()
                .getResource("haarcascade_eye.xml")
                .getPath();

            faceDetector = new CascadeClassifier(faceCascadePath);
            eyeDetector = new CascadeClassifier(eyeCascadePath);

            log.info("Video processing service initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize video processing: {}. Video features may be limited.", e.getMessage());
        }
    }

    /**
     * Process a video file and extract behavioral metrics
     */
    public Map<String, Object> processInterviewVideo(String videoPath) {
        Map<String, Object> metrics = new HashMap<>();
        List<Map<String, Object>> frameAnalyses = new ArrayList<>();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new File(videoPath))) {
            grabber.start();

            int frameCount = 0;
            int facesDetected = 0;
            int eyeContactFrames = 0;
            double totalConfidence = 0.0;

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                frameCount++;

                // Process every 30th frame (roughly 1 frame per second for 30fps video)
                if (frameCount % 30 == 0) {
                    Map<String, Object> frameMetrics = analyzeFrame(frame);
                    frameAnalyses.add(frameMetrics);

                    if ((boolean) frameMetrics.get("faceDetected")) {
                        facesDetected++;
                        totalConfidence += (double) frameMetrics.get("confidence");

                        if ((boolean) frameMetrics.get("eyeContact")) {
                            eyeContactFrames++;
                        }
                    }
                }
            }

            grabber.stop();

            // Calculate summary metrics
            int analyzedFrames = frameAnalyses.size();
            metrics.put("totalFrames", frameCount);
            metrics.put("analyzedFrames", analyzedFrames);
            metrics.put("faceDetectionRate", analyzedFrames > 0 ? (double) facesDetected / analyzedFrames : 0.0);
            metrics.put("eyeContactRate", facesDetected > 0 ? (double) eyeContactFrames / facesDetected : 0.0);
            metrics.put("averageConfidence", facesDetected > 0 ? totalConfidence / facesDetected : 0.0);
            metrics.put("frameAnalyses", frameAnalyses);
            metrics.put("duration", grabber.getLengthInTime() / 1000000.0); // Convert to seconds

            log.info("Processed video: {} frames, {} analyzed", frameCount, analyzedFrames);

        } catch (Exception e) {
            log.error("Error processing video", e);
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    /**
     * Analyze a single frame for face detection and eye contact
     */
    private Map<String, Object> analyzeFrame(Frame frame) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("faceDetected", false);
        metrics.put("eyeContact", false);
        metrics.put("confidence", 0.0);

        if (frame == null || faceDetector == null) {
            return metrics;
        }

        try {
            BufferedImage image = converter.convert(frame);
            if (image == null) {
                return metrics;
            }

            // Convert to Mat for OpenCV processing
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC3);
            Mat grayMat = new Mat();
            cvtColor(mat, grayMat, COLOR_BGR2GRAY);
            equalizeHist(grayMat, grayMat);

            // Detect faces
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(grayMat, faces);

            if (faces.size() > 0) {
                metrics.put("faceDetected", true);
                metrics.put("faceCount", faces.size());

                // Analyze the first (primary) face
                Rect face = faces.get(0);
                metrics.put("confidence", 0.8); // Placeholder - actual confidence would require additional processing

                // Detect eyes within face region
                Mat faceROI = new Mat(grayMat, face);
                RectVector eyes = new RectVector();
                eyeDetector.detectMultiScale(faceROI, eyes);

                // Eye contact heuristic: both eyes visible and properly positioned
                if (eyes.size() >= 2) {
                    metrics.put("eyeContact", true);
                    metrics.put("eyeCount", eyes.size());
                }
            }

        } catch (Exception e) {
            log.warn("Error analyzing frame: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * Extract a thumbnail from video at specified timestamp
     */
    public BufferedImage extractThumbnail(String videoPath, double timestampSeconds) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new File(videoPath))) {
            grabber.start();
            grabber.setTimestamp((long) (timestampSeconds * 1000000)); // Convert to microseconds

            Frame frame = grabber.grabImage();
            BufferedImage thumbnail = converter.convert(frame);

            grabber.stop();
            return thumbnail;

        } catch (Exception e) {
            log.error("Error extracting thumbnail", e);
            return null;
        }
    }

    /**
     * Get video metadata
     */
    public Map<String, Object> getVideoMetadata(String videoPath) {
        Map<String, Object> metadata = new HashMap<>();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new File(videoPath))) {
            grabber.start();

            metadata.put("duration", grabber.getLengthInTime() / 1000000.0); // seconds
            metadata.put("frameRate", grabber.getFrameRate());
            metadata.put("width", grabber.getImageWidth());
            metadata.put("height", grabber.getImageHeight());
            metadata.put("format", grabber.getFormat());
            metadata.put("videoBitrate", grabber.getVideoBitrate());
            metadata.put("audioChannels", grabber.getAudioChannels());

            grabber.stop();

        } catch (Exception e) {
            log.error("Error reading video metadata", e);
            metadata.put("error", e.getMessage());
        }

        return metadata;
    }
}
