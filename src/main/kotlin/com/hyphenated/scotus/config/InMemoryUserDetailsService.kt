package com.hyphenated.scotus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Profile("local", "test")
@Configuration
class InMemoryUserDetailsService(private val passwordEncoder: PasswordEncoder) {

  @Bean
  fun userDetailsService(): UserDetailsService {
    val user = User.withUsername("admin")
        .passwordEncoder(passwordEncoder::encode)
        .password("password")
        .roles("ADMIN")
        .build()
    return InMemoryUserDetailsManager(user)
  }
}