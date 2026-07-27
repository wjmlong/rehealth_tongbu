package com.rehealth.device.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class JdbcOutboxStoreSpringProxyTest {
    @Test
    void repositorySupportsSpringClassBasedExceptionTranslationProxy() {
        JdbcOutboxStore store = new JdbcOutboxStore(
                mock(JdbcTemplate.class),
                mock(TransactionTemplate.class)
        );
        ProxyFactory factory = new ProxyFactory(store);
        factory.setProxyTargetClass(true);

        Object proxy = factory.getProxy();

        assertInstanceOf(OutboxStore.class, proxy);
    }
}
