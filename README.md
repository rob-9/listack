# LeInterview - Java/Spring Boot

An AI-powered mock interview platform built with Java and Spring Boot that provides real-time feedback on technical and behavioral interviews.

## Features

- **Real-time AI Feedback**: Get instant feedback on your interview performance using Google's Gemini AI
- **Multi-language Code Execution**: Execute and test code in Python, Java, and C++ directly in the browser
- **Company-specific Questions**: Access interview questions tailored to specific companies
- **Kubernetes Support**: Optional Kubernetes-based code execution for scalable, secure sandboxing
- **Modern UI**: Responsive interface optimized for all devices
- **Secure**: Environment-based configuration with no hardcoded credentials

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Frontend**: HTML, CSS, JavaScript, Thymeleaf
- **AI**: Google Gemini API
- **Containerization**: Docker, Kubernetes (optional)
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (optional, for containerized deployment)
- Python 3, GCC/G++, and Java JDK (for code execution features)

## Quick Start

### Local Development

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd li_java
   ```

2. **Set up environment variables**:
   ```bash
   # Linux/Mac
   export GEMINI_API_KEY=your_api_key_here

   # Windows
   set GEMINI_API_KEY=your_api_key_here
   ```

3. **Build and run**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Access the application**:
   Open your browser and navigate to `http://localhost:5000`

### Docker Deployment

1. **Build the Docker image**:
   ```bash
   docker build -t leinterview-java .
   ```

2. **Run with Docker Compose**:
   ```bash
   docker-compose up -d
   ```

3. **Access the application**:
   Open your browser and navigate to `http://localhost:5000`

### Maven Build

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn clean package

# Run the JAR
java -jar target/leinterview-1.0.0.jar
```

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `GEMINI_API_KEY` | Your Google Gemini API key | No | `demo` |
| `DEMO_MODE` | Enable demo mode with mock responses | No | `true` |
| `AUDIO_ENABLED` | Enable audio transcription (not implemented in Java) | No | `false` |
| `USE_KUBERNETES` | Enable Kubernetes-based code execution | No | `false` |
| `KUBERNETES_NAMESPACE` | Kubernetes namespace for job execution | No | `interview-platform` |

### Application Properties

Configuration can be modified in `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=5000

# Gemini API
gemini.api-key=${GEMINI_API_KEY:demo}

# Interview Settings
interview.demo-mode=${DEMO_MODE:true}
interview.use-kubernetes=${USE_KUBERNETES:false}
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page |
| GET | `/interview` | Interview page |
| POST | `/start-interview` | Start interview session |
| POST | `/stop-interview` | Stop interview session |
| GET | `/get-feedback` | Get AI feedback on responses |
| POST | `/execute` | Execute code in various languages |
| GET | `/random-quest/{company}` | Get company-specific questions |
| GET | `/health` | Health check endpoint |
| GET | `/metrics` | Metrics endpoint |

## Project Structure

```
li_java/
├── src/
│   ├── main/
│   │   ├── java/com/leinterview/
│   │   │   ├── LeInterviewApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── InterviewController.java
│   │   │   │   └── PageController.java
│   │   │   └── service/
│   │   │       ├── CodeExecutionService.java
│   │   │       ├── GeminiService.java
│   │   │       ├── InterviewService.java
│   │   │       └── KubernetesCodeExecutor.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── companies.txt
│   │       ├── static/
│   │       │   └── Welcome.png
│   │       └── templates/
│   │           ├── index.html
│   │           └── interview.html
│   └── test/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Key Differences from Python Version

1. **Language**: Migrated from Python/Flask to Java/Spring Boot
2. **Dependency Management**: Using Maven instead of pip
3. **Template Engine**: Using Thymeleaf instead of Jinja2
4. **Type Safety**: Strong typing with Java vs. dynamic typing in Python
5. **Audio Support**: Audio transcription not implemented (requires platform-specific native libraries)
6. **Async Processing**: Using Spring's `@Async` for background processing

## Development

### Adding New Features

1. **Controllers**: Add REST endpoints in `controller/` package
2. **Services**: Add business logic in `service/` package
3. **Templates**: Add HTML templates in `src/main/resources/templates/`
4. **Static Files**: Add CSS/JS/images in `src/main/resources/static/`

### Code Execution

The application supports three languages:
- **Python**: Uses `python3` interpreter
- **Java**: Compiles with `javac` and runs with `java`
- **C++**: Compiles with `g++` and executes the binary

Code execution can be done either:
- **Locally**: Using subprocess execution (default)
- **Kubernetes**: Using Kubernetes Jobs for isolated execution (optional)

## Kubernetes Deployment

To enable Kubernetes-based code execution:

1. Set `USE_KUBERNETES=true`
2. Configure `KUBERNETES_NAMESPACE` (default: `interview-platform`)
3. Ensure the application has access to Kubernetes API
4. Deploy job templates in the `k8s/` directory (not included, needs to be ported)

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Troubleshooting

### Common Issues

1. **Port already in use**:
   - Change the port in `application.properties`: `server.port=8080`

2. **Gemini API errors**:
   - Verify your API key is set correctly
   - Check if you're hitting rate limits
   - Enable demo mode: `DEMO_MODE=true`

3. **Code execution failures**:
   - Ensure Python 3, Java JDK, and GCC are installed
   - Check that the PATH includes these compilers
   - Review logs for specific error messages

4. **Maven build failures**:
   - Ensure Java 17 is installed: `java -version`
   - Clear Maven cache: `mvn clean`
   - Update dependencies: `mvn clean install -U`

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

If you encounter any issues:
1. Check the logs: `tail -f logs/spring.log`
2. Review the troubleshooting section above
3. Open an issue on GitHub with detailed error information

## Acknowledgments

- Ported from the original Python/Flask implementation
- Uses Google Gemini for AI-powered feedback
- Built with Spring Boot and modern Java practices
