package org.clematis.logging.slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

/**
 * Class extending the {@link ThreadPoolExecutorFactoryBean} to propagate MDC context.
 */
public class MDCThreadPoolExecutorFactoryBean extends ThreadPoolExecutorFactoryBean {
    @Override
    protected ThreadPoolExecutor createExecutor(int corePoolSize,
                                                int maxPoolSize,
                                                int keepAliveSeconds,
                                                BlockingQueue<Runnable> queue,
                                                ThreadFactory threadFactory,
                                                RejectedExecutionHandler rejectedExecutionHandler
    ) {
        return new MDCThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS, queue,
            threadFactory, rejectedExecutionHandler);
    }
}