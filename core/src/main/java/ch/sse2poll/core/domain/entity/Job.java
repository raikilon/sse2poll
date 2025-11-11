package ch.sse2poll.core.domain.entity;

import java.time.Instant;

/**
 * Job aggregate with basic fields.
 */
public final class Job {
    private final String jobId;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Job(String jobId, String status, Instant createdAt, Instant updatedAt) {
        this.jobId = jobId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

