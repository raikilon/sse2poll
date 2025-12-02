package ch.sse2poll.core.framework.web;

import ch.sse2poll.core.engine.port.incoming.PollCoordinator;
import ch.sse2poll.core.framework.annotation.PolledGet;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolledGetAspectTest {

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void givenHttpRequest_WhenOrchestrate_ThenDerivesNamespaceAndContext() throws Throwable {
        Context ctx = Context.defaults();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("job", "jid-44");
        request.setParameter("waitMs", "250");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object res = ctx.aspect.orchestrate(ctx.joinPoint("fastEndpoint"), ctx.annotation("fastEndpoint"));

        assertEquals("fast", res);
        assertEquals("DemoController#fastEndpoint", ctx.coordinator.namespace);
        assertEquals("jid-44", ctx.coordinator.ctx.clientJobId());
        assertEquals(250L, ctx.coordinator.ctx.waitMs());
        assertEquals(1, ctx.coordinator.computeInvocations);
    }

    @Test
    void givenNoHttpRequest_WhenOrchestrate_ThenUsesDefaults() throws Throwable {
        Context ctx = Context.defaults();

        Object res = ctx.aspect.orchestrate(ctx.joinPoint("slowEndpoint"), ctx.annotation("slowEndpoint"));

        assertEquals("slow", res);
        assertEquals("DemoController#slowEndpoint", ctx.coordinator.namespace);
        assertNull(ctx.coordinator.ctx.clientJobId());
        assertEquals(0L, ctx.coordinator.ctx.waitMs());
        assertEquals(1, ctx.coordinator.computeInvocations);
    }

    static final class Context {
        final RecordingCoordinator coordinator = new RecordingCoordinator();
        final PolledGetAspect aspect = new PolledGetAspect(coordinator);

        static Context defaults() {
            return new Context();
        }

        MethodSignature signature(String method) throws NoSuchMethodException {
            Method m = DemoController.class.getDeclaredMethod(method);
            return new MethodSignature() {
                @Override
                public Method getMethod() {
                    return m;
                }

                @Override
                public Class<?> getReturnType() {
                    return m.getReturnType();
                }

                @Override
                public String[] getParameterNames() {
                    return new String[m.getParameterCount()];
                }

                @Override
                public Class<?>[] getParameterTypes() {
                    return m.getParameterTypes();
                }

                @Override
                public int getModifiers() {
                    return m.getModifiers();
                }

                @Override
                public String getName() {
                    return m.getName();
                }

                @Override
                public Class<?> getDeclaringType() {
                    return DemoController.class;
                }

                @Override
                public String getDeclaringTypeName() {
                    return DemoController.class.getName();
                }

                @Override
                public Class<?>[] getExceptionTypes() {
                    return m.getExceptionTypes();
                }

                @Override
                public String toShortString() {
                    return m.getName();
                }

                @Override
                public String toLongString() {
                    return m.toString();
                }

                @Override
                public String toString() {
                    return m.toString();
                }
            };
        }

        ProceedingJoinPoint joinPoint(String method) throws NoSuchMethodException {
            DemoController controller = new DemoController();
            Method m = DemoController.class.getDeclaredMethod(method);
            MethodSignature sig = signature(method);
            return new ProceedingJoinPoint() {
                @Override
                public String toShortString() {
                    return m.getName();
                }

                @Override
                public String toLongString() {
                    return m.toString();
                }

                @Override
                public Object getThis() {
                    return controller;
                }

                @Override
                public Object getTarget() {
                    return controller;
                }

                @Override
                public Object[] getArgs() {
                    return new Object[0];
                }

                @Override
                public MethodSignature getSignature() {
                    return sig;
                }

                @Override
                public SourceLocation getSourceLocation() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getKind() {
                    return "method-execution";
                }

                @Override
                public StaticPart getStaticPart() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Object proceed() throws Throwable {
                    return m.invoke(controller);
                }

                @Override
                public Object proceed(Object[] args) throws Throwable {
                    return m.invoke(controller, args);
                }

                @Override
                public void set$AroundClosure(AroundClosure arc) {
                    throw new UnsupportedOperationException("Unimplemented method 'set$AroundClosure'");
                }
            };
        }

        PolledGet annotation(String method) throws NoSuchMethodException {
            return DemoController.class.getDeclaredMethod(method).getAnnotation(PolledGet.class);
        }

        static final class RecordingCoordinator implements PollCoordinator {
            String namespace;
            RequestContextView ctx;
            int computeInvocations;

            @Override
            public <T> T handle(String namespace, Supplier<T> compute, Class<T> responseType, RequestContextView requestContext) {
                this.namespace = namespace;
                this.ctx = requestContext;
                computeInvocations++;
                return compute.get();
            }
        }

        static final class DemoController {
            @PolledGet
            String fastEndpoint() {
                return "fast";
            }

            @PolledGet
            String slowEndpoint() {
                return "slow";
            }
        }
    }
}
