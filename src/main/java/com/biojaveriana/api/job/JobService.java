package com.biojaveriana.api.job;

import java.time.Instant;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public JobService(JobRepository jobRepository, RedisTemplate<String, Object> redisTemplate) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
    }

    public Job createJob(String payload, String queueKey) {
        Job job = Job.builder()
                .payload(payload)
                .result(null)
                .errorMessage(null)
                .retries(0)
                .maxRetries(3)
                .status(JobStatus.PENDING)
                .queuedAt(null)
                .startedAt(null)
                .completedAt(null)
                .build();

        job = jobRepository.save(job);
        enqueueJob(job, queueKey);
        return job;
    }

    public Job createJob(String payload, int maxRetries, String queueKey) {
        Job job = Job.builder()
                .payload(payload)
                .result(null)
                .errorMessage(null)
                .retries(0)
                .maxRetries(maxRetries)
                .status(JobStatus.PENDING)
                .queuedAt(null)
                .startedAt(null)
                .completedAt(null)
                .build();

        job = jobRepository.save(job);
        enqueueJob(job, queueKey);
        return job;
    }

    private void enqueueJob(Job job, String queueKey) {
        job.setStatus(JobStatus.QUEUED);
        job.setQueuedAt(Instant.now());
        jobRepository.save(job);

        var payload = new java.util.HashMap<String, Object>();
        payload.put("id", job.getId().toString());
        redisTemplate.opsForList().rightPush(queueKey, payload);

        log.info("Job {} enqueued to Redis queue. Queue: {}", job.getId(), queueKey);
    }
}
