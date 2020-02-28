package com.hyphenated.scotus.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Helper config class to set up JPA Repositories
 *
 * This is deliberately separate from [com.hyphenated.scotus.ScotusTrackerApplication];
 * this is so all JPA repos don't need to be loaded for MVC testing.
 */
@Configuration
@EnableJpaRepositories("com.hyphenated.scotus")
class JpaConfig {
}