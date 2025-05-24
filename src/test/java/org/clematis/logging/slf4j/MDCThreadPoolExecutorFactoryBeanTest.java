package org.clematis.logging.slf4j;

import java.util.Objects;
import java.util.UUID;

import org.clematis.logging.slf4j.MDCThreadPoolExecutorFactoryBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Class to test the {@link MDCThreadPoolExecutorFactoryBean}
 */
public class MDCThreadPoolExecutorFactoryBeanTest {

    private static final String KEY = UUID.randomUUID().toString();
    private static final String VALUE = UUID.randomUUID().toString();
    private static final Runnable TASK = () -> Assertions.assertEquals(VALUE, MDC.get(KEY));

    MDCThreadPoolExecutorFactoryBean sut = new MDCThreadPoolExecutorFactoryBean();

    {
        sut.initialize();
    }

    @Test
    public void shouldPropagateMDCOnExecute() throws IllegalAccessException {
        //given
        MDC.put(KEY, VALUE);
        // when
        Objects.requireNonNull(sut.getObject()).execute(TASK);
    }

    @Test
    public void shouldPropagateMDCOnSubmit() throws IllegalAccessException {
        //given
        MDC.put(KEY, VALUE);
        // when
        Objects.requireNonNull(sut.getObject()).submit(TASK);
    }
}
