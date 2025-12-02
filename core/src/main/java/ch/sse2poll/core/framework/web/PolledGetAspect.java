package ch.sse2poll.core.framework.web;

import ch.sse2poll.core.engine.port.incoming.PollCoordinator;
import ch.sse2poll.core.framework.annotation.PolledGet;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect that routes controller methods annotated with {@link PolledGet} through the poll coordinator.
 */
@Aspect
@Component
public class PolledGetAspect {

    private static final long DEFAULT_WAIT_MS = 0L;

    private final PollCoordinator pollCoordinator;

    public PolledGetAspect(PollCoordinator pollCoordinator) {
        this.pollCoordinator = pollCoordinator;
    }

    @Around("@annotation(polledGet)")
    public <T> T orchestrate(ProceedingJoinPoint joinPoint, PolledGet polledGet) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        var method = methodSignature.getMethod();
        String namespace = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        PollCoordinator.RequestContextView requestContext = resolveRequestContext();

        Class<T> returnType = resolveReturnType(methodSignature);
        return pollCoordinator.handle(namespace, () -> proceed(joinPoint), returnType, requestContext);
    }

    private PollCoordinator.RequestContextView resolveRequestContext() {
        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null) {
            return new ImmutableRequestContext(null, DEFAULT_WAIT_MS);
        }
        HttpServletRequest request = attributes.getRequest();
        return new ImmutableRequestContext(extractJob(request), extractWaitMs(request));
    }

    private ServletRequestAttributes currentRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes;
        }
        return null;
    }

    private static String extractJob(HttpServletRequest request) {
        String jobParam = request.getParameter("job");
        if (jobParam == null || jobParam.isBlank()) {
            return null;
        }
        return jobParam;
    }

    private static long extractWaitMs(HttpServletRequest request) {
        String waitParam = request.getParameter("waitMs");
        if (waitParam == null || waitParam.isBlank()) {
            return DEFAULT_WAIT_MS;
        }
        try {
            long parsed = Long.parseLong(waitParam);
            return Math.max(DEFAULT_WAIT_MS, parsed);
        } catch (NumberFormatException ex) {
            return DEFAULT_WAIT_MS;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> resolveReturnType(MethodSignature methodSignature) {
        Class<?> raw = methodSignature.getReturnType();
        if (raw.isPrimitive()) {
            return (Class<T>) wrapPrimitive(raw);
        }
        return (Class<T>) raw;
    }

    @SuppressWarnings("unchecked")
    private <T> T proceed(ProceedingJoinPoint joinPoint) {
        try {
            return (T) joinPoint.proceed();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to execute polled computation", throwable);
        }
    }

    private Class<?> wrapPrimitive(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == char.class) return Character.class;
        if (primitive == void.class) return Void.class;
        return primitive;
    }

    private record ImmutableRequestContext(String clientJobId, long waitMs)
            implements PollCoordinator.RequestContextView {
    }
}
