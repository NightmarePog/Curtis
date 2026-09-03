package com.sosehl.curtis.platform.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;

class ReadOnlyTransactionTest {

    @Test
    void suppliesReadOnlyTransactionSemantics() throws NoSuchMethodException {
        Method method = QueryBoundary.class.getDeclaredMethod("query");
        TransactionAttribute transaction = new AnnotationTransactionAttributeSource()
            .getTransactionAttribute(method, QueryBoundary.class);

        assertNotNull(transaction);
        assertTrue(transaction.isReadOnly());
    }

    @Test
    void suppliesAClassDefaultThatCommandsCanOverride() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource source =
            new AnnotationTransactionAttributeSource();
        TransactionAttribute query = source.getTransactionAttribute(
            MixedBoundary.class.getDeclaredMethod("query"),
            MixedBoundary.class
        );
        TransactionAttribute command = source.getTransactionAttribute(
            MixedBoundary.class.getDeclaredMethod("command"),
            MixedBoundary.class
        );

        assertNotNull(query);
        assertTrue(query.isReadOnly());
        assertNotNull(command);
        assertFalse(command.isReadOnly());
    }

    static final class QueryBoundary {

        @SOSE_ReadOnlyTransaction
        public void query() {}
    }

    @SOSE_ReadOnlyTransaction
    static final class MixedBoundary {

        public void query() {}

        @Transactional
        public void command() {}
    }
}
