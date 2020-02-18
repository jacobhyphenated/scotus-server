package com.hyphenated.scotus.justice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface JusticeRepo: JpaRepository<Justice, Long> {

  @Query("select j from Justice j where j.dateRetired is null")
  fun findActive(): List<Justice>

  fun findByNameIgnoreCaseContaining(name: String): List<Justice>
}