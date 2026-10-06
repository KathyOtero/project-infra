package com.caribexperience.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita el llenado automatico de created_at/updated_at (BaseAuditEntity)
 * mediante los listeners @CreatedDate/@LastModifiedDate de Spring Data JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
