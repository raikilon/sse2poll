package ch.sse2poll.core.engine.exception;

/**
 * Thrown when a polled computation is still running and the caller should retry later.
 */
public final class PendingJobException extends RuntimeException {

    private final String jobId;

    public PendingJobException(String jobId) {
        super("Job " + jobId + " is still running");
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }
}
