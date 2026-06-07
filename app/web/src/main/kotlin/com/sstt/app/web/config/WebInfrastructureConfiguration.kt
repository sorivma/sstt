package com.sstt.app.web.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Web application infrastructure beans that are not owned by reusable data
 * modules.
 */
@Configuration(proxyBeanMethods = false)
class WebInfrastructureConfiguration {
    /**
     * Exposes the Reactor transaction operator required by the PostgreSQL
     * eventstore adapter.
     */
    @Bean
    fun transactionalOperator(transactionManager: ReactiveTransactionManager): TransactionalOperator {
        return TransactionalOperator.create(transactionManager)
    }
}
