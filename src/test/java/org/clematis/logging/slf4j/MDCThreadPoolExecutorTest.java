package org.clematis.logging.slf4j;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.clematis.logging.slf4j.MDCThreadPoolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Class to test the {@link MDCThreadPoolExecutor}
 */
public class MDCThreadPoolExecutorTest {

    private static final String KEY = UUID.randomUUID().toString();
    private static final String VALUE = UUID.randomUUID().toString();

    private final MDCThreadPoolExecutor sut = spy(new MDCThreadPoolExecutor(1, 1, 0,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>()));

    @Test
    public void shouldPropagateContextOnExecute() throws InterruptedException {
        // given
        MDC.put(KEY, VALUE);
        final Runnable cmd = () -> Assertions.assertEquals(VALUE, MDC.get(KEY));
        // when
        sut.execute(cmd);
        // then
        sut.shutdown();
        sut.awaitTermination(1, TimeUnit.MINUTES);
        verify(sut).beforeExecute(any(Thread.class), eq(cmd));
        verify(sut).afterExecute(cmd, null);
    }

    @Test
    public void shouldPropagateContextOnSubmit() throws ExecutionException, InterruptedException {
        // given
        MDC.put(KEY, VALUE);
        // when
        final Future<Object> future = sut.submit(() -> MDC.get(KEY));
        // then
        Assertions.assertEquals(VALUE, future.get());
        sut.shutdown();
        sut.awaitTermination(1, TimeUnit.MINUTES);
        verify(sut).beforeExecute(any(Thread.class), any(Runnable.class));
        verify(sut).afterExecute(any(Runnable.class), isNull());
    }

    @Test
    public void shouldPropagateContextOnRecursion() throws ExecutionException, InterruptedException {
        // given
        MDC.put(KEY, VALUE);
        // when
        final Future<Future<Object>> future1 = sut.submit(() -> {
            Assertions.assertEquals(VALUE, MDC.get(KEY));
            final Future<Object> result = sut.submit(() -> MDC.get(KEY));
            MDC.clear();
            return result;
        });
        // then
        final Future<Object> future2 = future1.get();
        Assertions.assertEquals(VALUE, future2.get());
        sut.shutdown();
        sut.awaitTermination(1, TimeUnit.MINUTES);
        verify(sut, times(2)).beforeExecute(any(Thread.class), any(Runnable.class));
        verify(sut, times(2)).afterExecute(any(Runnable.class), isNull());
    }

    @Test
    public void shouldPropagateContextOnSequence() throws ExecutionException, InterruptedException {
        // given
        MDC.put(KEY, VALUE);
        final Callable<Object> cmd = () -> MDC.get(KEY);
        // when
        final Future<Object> future1 = sut.submit(cmd);
        final Future<Object> future2 = sut.submit(cmd);
        // then
        Assertions.assertEquals(VALUE, future1.get());
        Assertions.assertEquals(VALUE, future2.get());
        sut.shutdown();
        sut.awaitTermination(1, TimeUnit.MINUTES);
        verify(sut, times(2)).beforeExecute(any(Thread.class), any(Runnable.class));
        verify(sut, times(2)).afterExecute(any(Runnable.class), isNull());
    }

    @Test
    public void testException() throws InterruptedException {
        // given
        final Exception e = new Exception();
        MDC.put(KEY, VALUE);
        final Callable<Object> cmd = () -> {
            Assertions.assertEquals(VALUE, MDC.get(KEY));
            throw e;
        };
        // when
        final Future<Object> future = sut.submit(cmd);
        // then
        try {
            future.get();
        } catch (ExecutionException result) {
            Assertions.assertEquals(e, result.getCause());
        }
        sut.shutdown();
        sut.awaitTermination(1, TimeUnit.MINUTES);
        verify(sut).beforeExecute(any(Thread.class), any(Runnable.class));
        verify(sut).afterExecute(any(Runnable.class), isNull());
    }
}
