package ch.sse2poll.core.engine.exception;

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
