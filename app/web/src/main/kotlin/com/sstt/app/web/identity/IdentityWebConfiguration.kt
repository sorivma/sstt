package com.sstt.app.web.identity

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Enables identity-related web runtime properties.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CurrentStudentProperties::class)
class IdentityWebConfiguration
