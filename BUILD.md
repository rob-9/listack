# Build Instructions

## Prerequisites

Ensure you have the following installed:
- Java 17 or higher
- Maven 3.6+
- Python 3 (for Python code execution)
- GCC/G++ (for C++ code execution)

## Build Steps

### 1. Verify Prerequisites

```bash
# Check Java version
java -version

# Check Maven version
mvn --version

# Check compilers
python3 --version
g++ --version
```

### 2. Build the Project

```bash
# Clean and compile
mvn clean compile

# Run tests (when implemented)
mvn test

# Package as JAR
mvn clean package

# Skip tests during packaging
mvn clean package -DskipTests
```

### 3. Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/leinterview-1.0.0.jar
```

### 4. Access the Application

Open your browser and navigate to:
```
http://localhost:5000
```

## Environment Variables

Set these before running:

```bash
# Optional: Set Gemini API key for real AI responses
export GEMINI_API_KEY=your_api_key_here

# Optional: Disable demo mode
export DEMO_MODE=false

# Optional: Enable Kubernetes execution
export USE_KUBERNETES=true
```

## Docker Build

```bash
# Build Docker image
docker build -t leinterview-java .

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

## Troubleshooting

### Maven not found
```bash
# macOS
brew install maven

# Ubuntu/Debian
sudo apt-get install maven

# Windows
# Download from https://maven.apache.org/download.cgi
```

### Compilation errors
```bash
# Clean Maven cache
mvn clean

# Force update dependencies
mvn clean install -U
```

### Port 5000 already in use
```bash
# Change port in application.properties
server.port=8080

# Or set environment variable
export SERVER_PORT=8080
```

## Next Steps

After successful build:
1. Test all endpoints using the API documentation in README.md
2. Verify code execution for Python, Java, and C++
3. Test with Gemini API key for AI feedback
4. Deploy using Docker or Kubernetes
