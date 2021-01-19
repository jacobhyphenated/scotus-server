package com.hyphenated.scotus.user

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class UserService(private val userRepo: UserRepo, private val passwordEncoder: PasswordEncoder) {

  fun getUserDetails(): UserInfo? {
    val authentication = SecurityContextHolder.getContext().authentication
    return (authentication?.principal as? UserDetails)?.toUserInfo()
  }

  @Transactional
  fun createUser(request: CreateUserRequest): UserInfo {
    userRepo.findOneByUsername(request.username)?.let { throw UsernameNotAvailable(request.username) }
    val user = userRepo.save(UserEntity(
      username = request.username,
      password =  passwordEncoder.encode(request.password)))
    return user.toUserDetails().toUserInfo()
  }
}

class UsernameNotAvailable(username: String):
  RuntimeException("A user with the username $username already exists")