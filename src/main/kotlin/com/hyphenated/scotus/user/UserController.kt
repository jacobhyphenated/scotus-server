package com.hyphenated.scotus.user

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

@RestController
@RequestMapping("user")
class UserController(private val userRepo: UserRepo, private val passwordEncoder: PasswordEncoder) {

  @GetMapping
  fun getUserDetails(): UserInfo? {
    val authentication = SecurityContextHolder.getContext().authentication
    return (authentication?.principal as? UserDetails)?.toUserInfo()
  }

  @Transactional
  @PostMapping
  fun createUser(@Valid @RequestBody request: CreateUserRequest): UserInfo {
    userRepo.findOneByUsername(request.username)?.let { throw UsernameNotAvailable(request.username) }
    val user = userRepo.save(UserEntity(
        username = request.username,
        password =  passwordEncoder.encode(request.password)))
    return user.toUserDetails().toUserInfo()
  }
}

class CreateUserRequest (
    @NotEmpty
    val username: String,
    @NotEmpty
    val password: String
)

class UsernameNotAvailable(username: String):
    RuntimeException("A user with the username $username already exists")