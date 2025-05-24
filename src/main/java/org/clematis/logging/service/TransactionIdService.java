package org.clematis.logging.service;

import javax.annotation.Nullable;

/**
 * Interface to define methods for transaction-ID management
 */
public interface TransactionIdService {

    String BEAN = "transactionIdService";
    /**
     * Starts transaction.
     * @param id transaction ID. If value is null than unique ID will be generated and assigned to result.
     * @return transaction object
     */
    Transaction startTransaction(@Nullable String id);

    /**
     * Returns active transaction's ID.
     * @return transaction ID or null
     */
    String getCurrentTransactionId();

    /**
     * Interface to define WPP transaction.
     *
     * @author <a href='mailto:ashchevaye@wiley.com'>Aleksey Shchevaev</a>
     */
    interface Transaction extends AutoCloseable {

        String id();

        @Override
        void close();
    }
}
