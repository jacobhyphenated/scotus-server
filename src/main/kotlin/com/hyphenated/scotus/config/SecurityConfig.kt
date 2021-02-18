package com.hyphenated.scotus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
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
class SecurityConfig(private val userDetailsService: UserDetailsService) : WebSecurityConfigurerAdapter() {

  @Bean
  fun unauthorizedEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint {
    _: HttpServletRequest?, response: HttpServletResponse, _: AuthenticationException? ->
      //use this header to prompt browser dialog
      response.addHeader("WWW-Authenticate", "Basic realm=SCOTUS Application")
      response.sendError(SC_UNAUTHORIZED)
  }

  @Bean(name = ["corsFilterBean"])
  fun corsFilterBean(): CorsFilter? {
    val source = UrlBasedCorsConfigurationSource()
    val config = CorsConfiguration()
    config.allowCredentials = true
    config.addAllowedOriginPattern("*")
    config.addAllowedHeader("*")
    config.addAllowedMethod("*")
    source.registerCorsConfiguration("/**", config)
    return CorsFilter(source)
  }

  override fun userDetailsService(): UserDetailsService {
    return userDetailsService
  }

  override fun configure(http: HttpSecurity) {
    http
        .authorizeRequests()
          // Add path specific authorizations to restrict
          .mvcMatchers("/docs/admin.html").hasRole("ADMIN")
          .mvcMatchers("/actuator/health").permitAll()
          .mvcMatchers("/actuator", "/actuator/**").hasRole("ADMIN")
          .antMatchers("/h2-console", "/h2-console/**").hasRole("ADMIN")
          .anyRequest().permitAll()
          .and()
        .httpBasic()
          .and()
        .addFilterBefore(corsFilterBean(), ChannelProcessingFilter::class.java)
        .exceptionHandling()
          .authenticationEntryPoint(unauthorizedEntryPoint())
          .and()
        // CSRF protection is not necessary because this service does not depend on session cookies.
        // Authenticated requests require a header or other user input that cannot be faked via CSRF.
        .csrf().disable()
        .headers()
          .frameOptions().sameOrigin()
          .and()
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
        .anonymous()
  }

}