package com.leinterview.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KubernetesCodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(KubernetesCodeExecutor.class);

    @Value("${kubernetes.namespace:interview-platform}")
    private String namespace;

    @Value("${interview.use-kubernetes:false}")
    private boolean useKubernetes;

    private BatchV1Api batchApi;
    private CoreV1Api coreApi;

    @PostConstruct
    public void init() {
        if (!useKubernetes) {
            log.info("Kubernetes execution disabled");
            return;
        }

        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            batchApi = new BatchV1Api();
            coreApi = new CoreV1Api();

            log.info("Kubernetes executor initialized successfully");
        } catch (IOException e) {
            log.warn("Failed to initialize Kubernetes client: {}. Kubernetes features disabled.", e.getMessage());
            useKubernetes = false;
        }
    }

    public Map<String, String> executeCode(String code, String language, int timeoutSeconds) {
        if (!useKubernetes) {
            throw new IllegalStateException("Kubernetes is not enabled");
        }

        String jobId = UUID.randomUUID().toString().substring(0, 8);
        String jobName = language + "-executor-" + jobId;

        try {
            // Create job
            V1Job job = createJob(jobName, language, code);
            batchApi.createNamespacedJob(namespace, job, null, null, null, null);

            // Wait for completion
            String status = waitForJobCompletion(jobName, timeoutSeconds);

            // Get logs
            String output = getJobLogs(jobName);

            // Cleanup
            deleteJob(jobName);

            Map<String, String> result = new HashMap<>();
            result.put("output", output);
            result.put("error", "succeeded".equals(status) ? "" : "Job failed");
            result.put("status", status);

            return result;

        } catch (ApiException e) {
            log.error("Kubernetes API error", e);
            Map<String, String> result = new HashMap<>();
            result.put("output", "");
            result.put("error", "Kubernetes API error: " + e.getResponseBody());
            result.put("status", "failed");
            return result;
        } catch (Exception e) {
            log.error("Error executing code in Kubernetes", e);
            Map<String, String> result = new HashMap<>();
            result.put("output", "");
            result.put("error", e.getMessage());
            result.put("status", "failed");
            return result;
        }
    }

    private V1Job createJob(String jobName, String language, String code) {
        V1Job job = new V1Job();
        job.setApiVersion("batch/v1");
        job.setKind("Job");

        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(jobName);
        Map<String, String> labels = new HashMap<>();
        labels.put("role", "code-executor");
        labels.put("language", language);
        metadata.setLabels(labels);
        job.setMetadata(metadata);

        V1JobSpec spec = new V1JobSpec();
        spec.setBackoffLimit(0);
        spec.setTtlSecondsAfterFinished(300);

        V1PodTemplateSpec template = new V1PodTemplateSpec();
        V1ObjectMeta templateMetadata = new V1ObjectMeta();
        templateMetadata.setLabels(labels);
        template.setMetadata(templateMetadata);

        V1PodSpec podSpec = new V1PodSpec();
        podSpec.setRestartPolicy("Never");

        V1Container container = createContainer(language, code);
        podSpec.addContainersItem(container);

        template.setSpec(podSpec);
        spec.setTemplate(template);
        job.setSpec(spec);

        return job;
    }

    private V1Container createContainer(String language, String code) {
        V1Container container = new V1Container();
        container.setName("code-runner");

        switch (language.toLowerCase()) {
            case "python":
                container.setImage("python:3.11-slim");
                container.addCommandItem("python3");
                container.addArgsItem("-c");
                container.addArgsItem(code);
                break;

            case "java":
                container.setImage("openjdk:17-slim");
                container.addCommandItem("/bin/sh");
                container.addArgsItem("-c");
                String javaCmd = "echo '" + code.replace("'", "'\\''") + "' > Main.java && javac Main.java && java Main";
                container.addArgsItem(javaCmd);
                break;

            case "c++":
                container.setImage("gcc:latest");
                container.addCommandItem("/bin/sh");
                container.addArgsItem("-c");
                String cppCmd = "echo '" + code.replace("'", "'\\''") + "' > main.cpp && g++ main.cpp -o main && ./main";
                container.addArgsItem(cppCmd);
                break;

            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }

        V1ResourceRequirements resources = new V1ResourceRequirements();
        Map<String, io.kubernetes.client.custom.Quantity> limits = new HashMap<>();
        limits.put("cpu", new io.kubernetes.client.custom.Quantity("500m"));
        limits.put("memory", new io.kubernetes.client.custom.Quantity("512Mi"));
        resources.setLimits(limits);
        container.setResources(resources);

        return container;
    }

    private String waitForJobCompletion(String jobName, int timeoutSeconds) throws ApiException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            V1Job job = batchApi.readNamespacedJobStatus(jobName, namespace, null);
            V1JobStatus status = job.getStatus();

            if (status != null) {
                if (status.getSucceeded() != null && status.getSucceeded() > 0) {
                    return "succeeded";
                }

                if (status.getFailed() != null && status.getFailed() > 0) {
                    return "failed";
                }
            }

            Thread.sleep(500);
        }

        return "timeout";
    }

    private String getJobLogs(String jobName) {
        try {
            String labelSelector = "job-name=" + jobName;
            V1PodList podList = coreApi.listNamespacedPod(
                    namespace,           // namespace
                    null,               // pretty
                    null,               // allowWatchBookmarks
                    null,               // _continue
                    null,               // fieldSelector
                    labelSelector,      // labelSelector
                    null,               // limit
                    null,               // resourceVersion
                    null,               // resourceVersionMatch
                    null,               // sendInitialEvents
                    null,               // timeoutSeconds
                    null                // watch
            );

            if (podList.getItems().isEmpty()) {
                return "No pods found for job";
            }

            V1Pod pod = podList.getItems().get(0);
            String podName = pod.getMetadata().getName();
            String containerName = pod.getSpec().getContainers().get(0).getName();

            return coreApi.readNamespacedPodLog(podName, namespace, containerName, null, null,
                    null, null, null, null, null, null);

        } catch (ApiException e) {
            log.error("Error getting pod logs", e);
            return "Error getting logs: " + e.getResponseBody();
        }
    }

    private void deleteJob(String jobName) {
        try {
            batchApi.deleteNamespacedJob(jobName, namespace, null, null, null, null,
                    "Foreground", null);
        } catch (ApiException e) {
            log.warn("Error deleting job {}: {}", jobName, e.getMessage());
        }
    }

    public int getActiveJobs() {
        if (!useKubernetes) {
            return 0;
        }

        try {
            String labelSelector = "role=code-executor";
            V1JobList jobList = batchApi.listNamespacedJob(
                    namespace,           // namespace
                    null,               // pretty
                    null,               // allowWatchBookmarks
                    null,               // _continue
                    null,               // fieldSelector
                    labelSelector,      // labelSelector
                    null,               // limit
                    null,               // resourceVersion
                    null,               // resourceVersionMatch
                    null,               // sendInitialEvents
                    null,               // timeoutSeconds
                    null                // watch
            );

            return (int) jobList.getItems().stream()
                    .filter(job -> job.getStatus() != null && job.getStatus().getActive() != null && job.getStatus().getActive() > 0)
                    .count();
        } catch (ApiException e) {
            log.error("Error getting active jobs", e);
            return 0;
        }
    }

    public void cleanupOldJobs(int maxAgeSeconds) {
        if (!useKubernetes) {
            return;
        }

        try {
            String labelSelector = "role=code-executor";
            V1JobList jobList = batchApi.listNamespacedJob(
                    namespace,           // namespace
                    null,               // pretty
                    null,               // allowWatchBookmarks
                    null,               // _continue
                    null,               // fieldSelector
                    labelSelector,      // labelSelector
                    null,               // limit
                    null,               // resourceVersion
                    null,               // resourceVersionMatch
                    null,               // sendInitialEvents
                    null,               // timeoutSeconds
                    null                // watch
            );

            OffsetDateTime now = OffsetDateTime.now();

            for (V1Job job : jobList.getItems()) {
                OffsetDateTime creationTime = job.getMetadata().getCreationTimestamp();
                if (creationTime != null) {
                    long ageSeconds = java.time.Duration.between(creationTime, now).getSeconds();
                    if (ageSeconds > maxAgeSeconds) {
                        deleteJob(job.getMetadata().getName());
                    }
                }
            }
        } catch (ApiException e) {
            log.warn("Error cleaning up old jobs: {}", e.getMessage());
        }
    }
}
