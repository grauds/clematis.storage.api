package org.clematis.logging.service.impl;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.clematis.logging.service.TransactionIdService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class implements the {@link TransactionIdService} interface.
 */
@Component(value = TransactionIdService.BEAN)
public class TransactionIdServiceImpl implements TransactionIdService {

    @Value("${clematis.transactionId.logKey}")
    private String transactionIdLogKey;

    @Override
    public Transaction startTransaction(@Nullable String id) {
        final String transactionId = StringUtils.isBlank(id) ? generateId() : id;
        return new TransactionImpl(transactionIdLogKey, transactionId);
    }

    @Override
    public String getCurrentTransactionId() {
        return MDC.get(transactionIdLogKey);
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Class implements the {@link Transaction} interface.
     */
    private record TransactionImpl(String mdcKey, String id) implements Transaction {

        private TransactionImpl(String mdcKey, String id) {
            this.mdcKey = mdcKey;
            this.id = id;
            MDC.put(mdcKey, id);
        }

        @Override
        public void close() {
            MDC.remove(mdcKey);
        }

        @Override
        public String toString() {
            return "TransactionImpl{id='" + id + "'}";
        }
    }
}
