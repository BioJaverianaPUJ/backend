package com.biojaveriana.api.job;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobConsumer {
    private final JobService jobService;

    /**
     * Consumes JOB_CREATED messages from the queue and starts processing.
     * The message body is the JSON-serialized map containing "id" (the job UUID string).
     */
    @RabbitListener(queues = "${app.rabbitmq.queue-names}")
    public void consumeJobMessage(String message) {
        try {
            // Parse the job ID from the message payload
            String jobIdStr = extractJobId(message);
            UUID jobId = UUID.fromString(jobIdStr);

            log.info("Received job message for Job {}", jobId);

            // Transition QUEUED → PROCESSING and start handling
            jobService.startProcessing(jobId);

            // Determine the handler for this job type and execute it
            String result = handleJob(jobId);

            jobService.completeJob(jobId, result);
            log.info("Job {} completed successfully", jobId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid message format: {}", message, e);
        } catch (Exception e) {
            log.error("Failed to process job from message: {}", message, e);
            throw e; // Let RabbitMQ redeliver or send to DLQ
        }
    }

    private String extractJobId(String message) {
        // The OutboxProcessor sends the payload as a Map serialized to JSON.
        // For JOB_CREATED events, the map contains "id" -> job UUID string.
        // Try parsing as JSON map first, then fall back to plain string.
        if (message.startsWith("{")) {
            try {
                // Remove braces and quotes to get simple key:value pairs
                String trimmed = message.trim().replaceAll("[{}\"]", "");
                for (String pair : trimmed.split(",")) {
                    if (pair.contains("id")) {
                        return pair.split(":")[1].trim();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse JSON payload, treating as raw ID");
            }
        }
        return message.trim().replaceAll("[{}\"\"]", "");
    }

    /**
     * Dispatches job handling based on the queue key / resource type.
     */
    private String handleJob(UUID jobId) {
        var job = jobService.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        return switch (job.getQueueKey()) {
            case "jobs:observation:identification" -> handleObservationIdentification(jobId, job);
            default -> {
                log.warn("No handler for queue key '{}', treating as completed", job.getQueueKey());
                yield "{}"; // Default: treat unknown jobs as completed successfully
            }
        };
    }

    private String handleObservationIdentification(UUID jobId, Job job) {
        try {
            // Extract observation ID from the payload (set when job was created)
            String resourceId = extractResourceId(job);

            if (resourceId == null || resourceId.isEmpty()) {
                log.warn("No resource ID in job {} — skipping identification", jobId);
                return "{}";
            }

            log.info("Processing observation identification for resource: {}", resourceId);

            // Perform the identification logic here
            String result = performIdentification(resourceId, job.getPayload());

            return result;

        } catch (Exception e) {
            log.error("Observation identification failed for jobId={}", jobId, e);
            throw new RuntimeException("Observation identification failed", e);
        }
    }

    /**
     * Placeholder for actual identification logic.
     * In production, this would invoke the image analysis / ML model service.
     */
    private String performIdentification(String resourceId, String payload) {
        // TODO: Integrate with the actual identification service (e.g., Bio-Javeriana AI/ML pipeline)
        // For now, return a mock result for testing purposes
        log.info("Performing identification for resource {}: placeholder result", resourceId);

        // Example mock response structure
        return """
            {
              "resourceId": "%s",
              "identificationType": "observation",
              "status": "completed",
              "confidence": 0.85,
              "result": "Sample identification result"
            }
            """.formatted(resourceId);
    }

    /**
     * Extracts the owning resource ID from the job's payload or polymorphic fields.
     */
    private String extractResourceId(Job job) {
        // Prefer polymorphic fields if available
        if (job.getResourceId() != null) {
            return job.getResourceId().toString();
        }

        // Fall back to parsing from payload JSON
        try {
            String trimmed = job.getPayload().trim().replaceAll("[{}\"]", "");
            for (String pair : trimmed.split(",")) {
                if (pair.contains("observationId") || pair.contains("id")) {
                    return pair.split(":")[1].trim();
                }
            }
        } catch (Exception e) {
            // Payload may be plain text; ignore errors here.
        }

        return null;
    }
}
