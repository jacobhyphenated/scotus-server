package com.hyphenated.scotus.config

import com.hyphenated.scotus.user.UserRepo
import com.hyphenated.scotus.user.toUserDetails
import org.springframework.context.annotation.Profile
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Profile("dev", "prod")
@Service
class DatabaseUserDetailsService(private val userRepo: UserRepo): UserDetailsService {

  override fun loadUserByUsername(username: String): UserDetails {
    return userRepo.findOneByUsername(username)?.toUserDetails()
        ?: throw UsernameNotFoundException("User with name $username not found")
  }

}