package com.hyphenated.scotus.user

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("user")
class UserController {

  @GetMapping
  fun getUserDetails(): UserInfo? {
    val authentication = SecurityContextHolder.getContext().authentication
    return (authentication?.principal as? UserDetails)?.let {
      UserInfo(it.username, it.authorities.map { authority -> authority.toString() })
    }
  }
}

