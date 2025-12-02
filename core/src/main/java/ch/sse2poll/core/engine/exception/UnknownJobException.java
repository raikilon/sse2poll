package ch.sse2poll.core.engine.exception;

/**
 * Thrown when a requested job id is missing from the coordinator cache.
 */
public final class UnknownJobException extends RuntimeException {

    private final String jobId;

    public UnknownJobException(String jobId) {
        super("Unknown job id: " + jobId);
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }
}
