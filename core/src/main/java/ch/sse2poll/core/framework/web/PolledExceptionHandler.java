package ch.sse2poll.core.framework.web;

import ch.sse2poll.core.engine.exception.PendingJobException;
import ch.sse2poll.core.engine.exception.UnknownJobException;
import ch.sse2poll.core.entities.model.Pending;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PolledExceptionHandler {

    @ExceptionHandler(PendingJobException.class)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Pending handlePending(PendingJobException ex) {
        return new Pending(ex.getJobId());
    }

    @ExceptionHandler(UnknownJobException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUnknown(UnknownJobException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
