package com.hyphenated.scotus.justice

import org.springframework.data.jpa.repository.JpaRepository

interface JusticeRepo: JpaRepository<Justice, Long> {

  fun findByDateRetiredIsNull(): List<Justice>

  fun findByNameIgnoreCaseContaining(name: String): List<Justice>
}