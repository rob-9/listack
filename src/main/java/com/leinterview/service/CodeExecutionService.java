package com.leinterview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 10;

    public String executeCode(String code, String language) {
        try {
            return switch (language.toLowerCase()) {
                case "python" -> executePython(code);
                case "java" -> executeJava(code);
                case "c++" -> executeCpp(code);
                default -> "Unsupported language";
            };
        } catch (Exception e) {
            log.error("Error executing code", e);
            return "Error: " + e.getMessage();
        }
    }

    private String executePython(String code) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python3", "-c", code);
        return executeProcess(pb);
    }

    private String executeJava(String code) throws Exception {
        // Create temporary directory
        Path tempDir = Files.createTempDirectory("java_exec");
        Path javaFile = tempDir.resolve("Main.java");

        try {
            // Write code to file
            Files.writeString(javaFile, code, StandardOpenOption.CREATE);

            // Compile
            ProcessBuilder compilePb = new ProcessBuilder("javac", javaFile.toString());
            compilePb.directory(tempDir.toFile());
            String compileOutput = executeProcess(compilePb);

            if (!compileOutput.isEmpty() && compileOutput.toLowerCase().contains("error")) {
                return compileOutput;
            }

            // Run
            ProcessBuilder runPb = new ProcessBuilder("java", "-cp", tempDir.toString(), "Main");
            runPb.directory(tempDir.toFile());
            return executeProcess(runPb);

        } finally {
            // Cleanup
            deleteDirectory(tempDir.toFile());
        }
    }

    private String executeCpp(String code) throws Exception {
        // Create temporary directory
        Path tempDir = Files.createTempDirectory("cpp_exec");
        Path cppFile = tempDir.resolve("main.cpp");
        Path executable = tempDir.resolve("main");

        try {
            // Write code to file
            Files.writeString(cppFile, code, StandardOpenOption.CREATE);

            // Compile
            ProcessBuilder compilePb = new ProcessBuilder("g++", cppFile.toString(), "-o", executable.toString());
            compilePb.directory(tempDir.toFile());
            String compileOutput = executeProcess(compilePb);

            if (!compileOutput.isEmpty() && compileOutput.toLowerCase().contains("error")) {
                return compileOutput;
            }

            // Run
            ProcessBuilder runPb = new ProcessBuilder(executable.toString());
            runPb.directory(tempDir.toFile());
            return executeProcess(runPb);

        } finally {
            // Cleanup
            deleteDirectory(tempDir.toFile());
        }
    }

    private String executeProcess(ProcessBuilder pb) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            return "Code execution timed out";
        }

        if (process.exitValue() != 0 && output.length() == 0) {
            return "Process exited with code: " + process.exitValue();
        }

        return output.toString();
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
