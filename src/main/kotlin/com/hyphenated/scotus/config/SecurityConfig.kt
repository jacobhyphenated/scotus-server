package com.hyphenated.scotus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val env: Environment): WebSecurityConfigurerAdapter() {

  @Bean
  fun unauthorizedEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint {
    _: HttpServletRequest?, response: HttpServletResponse, _: AuthenticationException? ->
    response.sendError(SC_UNAUTHORIZED)
  }

  @Bean(name = ["corsFilterBean"])
  fun corsFilterBean(): CorsFilter? {
    val source = UrlBasedCorsConfigurationSource()
    val config = CorsConfiguration()
    config.allowCredentials = true
    config.addAllowedOrigin("*")
    config.addAllowedHeader("*")
    config.addAllowedMethod("*")
    source.registerCorsConfiguration("/**", config)
    return CorsFilter(source)
  }

  @Bean
  /**
   * Create an admin user for local testing only
   */
  override fun userDetailsService(): UserDetailsService {
    if (env.activeProfiles.contains("local") || env.activeProfiles.contains("test")) {
      val user = User.withDefaultPasswordEncoder()
          .username("admin")
          .password("password")
          .roles("ADMIN")
          .build()
      return InMemoryUserDetailsManager(user)
    }

    // TODO: real auth manager
    return InMemoryUserDetailsManager()

  }

  override fun configure(http: HttpSecurity) {
    http
        .authorizeRequests()
          // Add path specific authorizations to restrict
          .anyRequest().permitAll()
          .and()
        .httpBasic()
          .realmName("SCOTUS Application")
          .and()
        .addFilterBefore(corsFilterBean(), ChannelProcessingFilter::class.java)
        .exceptionHandling()
          .authenticationEntryPoint(unauthorizedEntryPoint())
          .and()
        //CSRF protection is not necessary because this service does not depend on session cookies.
        //Authenticated requests require a header or other user input that cannot be faked via CSRF.
        .csrf().disable()
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
        .anonymous()
  }

}