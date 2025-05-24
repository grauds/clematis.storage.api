package org.clematis.logging.slf4j;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

/**
 * Class extending the {@link ThreadPoolExecutor} to propagate MDC context.
 */
public class MDCThreadPoolExecutor extends ThreadPoolExecutor {

    private final Map<Runnable, Context> contexts = new ConcurrentHashMap<>();

    public MDCThreadPoolExecutor(int corePoolSize,
                                 int maximumPoolSize,
                                 long keepAliveTime,
                                 TimeUnit unit,
                                 BlockingQueue<Runnable> workQueue,
                                 ThreadFactory threadFactory,
                                 RejectedExecutionHandler handler
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    MDCThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public void execute(Runnable command) {
        contexts.put(command, new Context());
        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        final Context context = contexts.get(r);
        if (context != null) {
            context.pushContext(t);
        }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        final Context context = contexts.remove(r);
        if (context != null) {
            context.popContext();
        }
    }

    /**
     * Class to temporary h MDC context
     */
    static class Context {

        private final Map<String, String> commandContext;
        private Map<String, String> savedContext;
        private boolean pushed = false;

        Context() {
            commandContext = MDC.getCopyOfContextMap();
        }

        private void setContext(Map<String, String> context) {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
        }

        void pushContext(Thread ignored) {
            if (!pushed) {
                savedContext = MDC.getCopyOfContextMap();
                setContext(commandContext);
                pushed = true;
            }
        }

        void popContext() {
            if (pushed) {
                pushed = false;
                setContext(savedContext);
            }
        }
    }
}
