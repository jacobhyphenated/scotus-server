package com.hyphenated.scotus.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepo: JpaRepository<UserEntity, String> {
  fun findOneByUsername(username: String): UserEntity?
}