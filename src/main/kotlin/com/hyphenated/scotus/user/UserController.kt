package com.hyphenated.scotus.user

import org.springframework.web.bind.annotation.*
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@RestController
@RequestMapping("user")
class UserController(private val userService: UserService) {

  @GetMapping
  fun getUserDetails(): UserInfo? {
    return userService.getUserDetails()
  }

  @Transactional
  @PostMapping
  fun createUser(@Valid @RequestBody request: CreateUserRequest): UserInfo {
    return userService.createUser(request)
  }
}

class CreateUserRequest (
    @NotEmpty
    val username: String,
    @NotEmpty
    val password: String
)