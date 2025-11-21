# OpenCV/JavaCV Integration

This document describes the video processing and behavioral metrics features integrated into the LeInterview platform using JavaCV.

## Features

### 1. Video Processing Service
- **Face Detection**: Detects faces in video frames using Haar Cascade classifiers
- **Eye Contact Tracking**: Monitors eye presence and positioning to estimate engagement
- **Frame Analysis**: Processes video frames to extract behavioral metrics
- **Thumbnail Extraction**: Extracts still images from videos at specific timestamps
- **Video Metadata**: Retrieves video properties (duration, resolution, bitrate, etc.)

### 2. Behavioral Metrics Analysis
- **Engagement Score**: 0-100 score based on face detection, eye contact, and confidence
- **Face Detection Rate**: Percentage of frames where candidate's face is visible
- **Eye Contact Rate**: Percentage of time maintaining eye contact with camera
- **Performance Assessment**: Automated categorization (Excellent/Good/Fair/Needs Improvement)
- **Personalized Feedback**: Generated based on specific behavioral patterns

### 3. REST API Endpoints

#### Analyze Video
```bash
POST /api/video/analyze
Content-Type: multipart/form-data

# Upload video file for complete behavioral analysis
curl -X POST http://localhost:8080/api/video/analyze \
  -F "file=@interview.mp4"
```

**Response:**
```json
{
  "status": "success",
  "videoId": "uuid-interview.mp4",
  "analysis": {
    "engagementScore": 75.5,
    "faceDetectionRate": 95.2,
    "eyeContactRate": 68.3,
    "duration": 180.5,
    "assessment": "Good",
    "feedback": "Great eye contact - this shows confidence and engagement. Excellent camera positioning throughout the interview.",
    "rawMetrics": {
      "totalFrames": 5415,
      "analyzedFrames": 180,
      "frameAnalyses": [...]
    }
  }
}
```

#### Get Video Metadata
```bash
POST /api/video/metadata
Content-Type: multipart/form-data

curl -X POST http://localhost:8080/api/video/metadata \
  -F "file=@interview.mp4"
```

**Response:**
```json
{
  "duration": 180.5,
  "frameRate": 30.0,
  "width": 1920,
  "height": 1080,
  "format": "mov,mp4,m4a,3gp,3g2,mj2",
  "videoBitrate": 5000000,
  "audioChannels": 2
}
```

#### Extract Thumbnail
```bash
POST /api/video/thumbnail?timestamp=30.0
Content-Type: multipart/form-data

curl -X POST "http://localhost:8080/api/video/thumbnail?timestamp=30.0" \
  -F "file=@interview.mp4" \
  -o thumbnail.png
```

Returns a PNG image at the specified timestamp.

#### Health Check
```bash
GET /api/video/health

curl http://localhost:8080/api/video/health
```

## Dependencies

The integration uses the following libraries:

- **JavaCV**: 1.5.10 - Java interface to OpenCV and FFmpeg
- **OpenCV**: 4.9.0 - Computer vision library for face/eye detection
- **FFmpeg**: Bundled with JavaCV - Video processing and frame extraction

All native libraries are bundled with the `-platform` artifacts, making deployment seamless across Linux, macOS, and Windows.

## Configuration

No additional configuration is required. The service automatically:
- Downloads and uses bundled Haar Cascade classifiers
- Creates temporary directories for video uploads
- Cleans up processed videos after analysis

## Technical Details

### How It Works

1. **Video Upload**: Videos are temporarily stored in system temp directory
2. **Frame Extraction**: FFmpegFrameGrabber extracts frames at 1 FPS intervals
3. **Face Detection**: OpenCV Haar Cascades detect faces in each frame
4. **Eye Detection**: Within detected faces, eyes are located for engagement metrics
5. **Metric Calculation**: Weighted algorithm produces engagement score:
   - Face Detection Rate: 30% weight
   - Eye Contact Rate: 50% weight
   - Confidence: 20% weight
6. **Cleanup**: Temporary video files are deleted after processing

### Haar Cascade Classifiers

Pre-trained classifiers are included:
- `haarcascade_frontalface_default.xml` - Frontal face detection
- `haarcascade_eye.xml` - Eye detection

## Resume Bullet Points

This implementation supports the following resume claims:

- ✅ "Integrated OpenCV for video processing and behavioral metrics tracking"
- ✅ "Implemented face detection and eye contact analysis for interview feedback"
- ✅ "Built REST API for automated video analysis with engagement scoring"
- ✅ "Developed behavioral metrics system using computer vision algorithms"

## Future Enhancements

Potential improvements:
- Real-time video processing via WebSockets
- Emotion detection using deep learning models
- Speech analysis integration
- Multi-face tracking for panel interviews
- Background distraction analysis
- Posture and gesture recognition

## Deployment Notes

### Docker/Kubernetes
JavaCV platform artifacts include native libraries for all platforms, making containerization straightforward. No additional system dependencies needed.

### Memory Requirements
Video processing is memory-intensive. Recommended:
- Minimum: 512MB RAM per concurrent analysis
- Recommended: 1GB+ RAM for optimal performance

### Supported Video Formats
FFmpeg supports: MP4, MOV, AVI, MKV, WebM, FLV, and most common formats.
