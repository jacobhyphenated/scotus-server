package com.hyphenated.scotus.user

import org.springframework.security.core.userdetails.UserDetails

data class UserInfo(
    val username: String,
    val roles: List<String>
)

fun UserDetails.toUserInfo(): UserInfo {
  return UserInfo(username, authorities.map { authority -> authority.toString() })
}