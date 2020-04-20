package com.hyphenated.scotus.case

import org.springframework.data.jpa.repository.JpaRepository

interface CaseRepo: JpaRepository<Case, Long> {

  fun findByTermId(termId: Long): List<Case>

  fun findByCaseIgnoreCaseContaining(title: String): List<Case>
}