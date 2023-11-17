package com.hyphenated.scotus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher
import org.springframework.web.servlet.handler.HandlerMappingIntrospector


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig() {

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

  @Bean
  fun mvc(introspector: HandlerMappingIntrospector): MvcRequestMatcher.Builder {
    return MvcRequestMatcher.Builder(introspector)
  }

    @Bean
  fun configureSecurityChain(http: HttpSecurity, mvc: MvcRequestMatcher.Builder ): SecurityFilterChain {
    http
        .authorizeHttpRequests { authorize ->
          authorize
            .requestMatchers(mvc.pattern("/docs/admin.html")).hasRole("ADMIN")
            .requestMatchers(mvc.pattern("/actuator/health")).permitAll()
            .requestMatchers(mvc.pattern("/actuator"), mvc.pattern("/actuator/**")).hasRole("ADMIN")
            .requestMatchers(mvc.pattern("/h2-console"), mvc.pattern("/h2-console/**")).hasRole("ADMIN")
            .anyRequest().permitAll()
        }

        .httpBasic(Customizer.withDefaults())
        .addFilterBefore(corsFilterBean(), ChannelProcessingFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(unauthorizedEntryPoint()) }
        // CSRF protection is not necessary because this service does not depend on session cookies.
        // Authenticated requests require a header or other user input that cannot be faked via CSRF.
        .csrf { it.disable() }
        .headers { headers ->
          headers.frameOptions { it.sameOrigin() }
        }
        .sessionManagement {
          it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }
        .anonymous { }
    return http.build()
  }

}