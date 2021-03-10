package com.hyphenated.scotus.case

import org.springframework.data.jpa.repository.JpaRepository

interface CaseRepo: JpaRepository<Case, Long> {

  fun findByTermId(termId: Long): List<Case>

  fun findByCaseIgnoreCaseContaining(title: String): List<Case>

  fun findByIdIn(ids: List<Long>): List<Case>

  fun findByAlternateTitles_titleIgnoreCaseContaining(title: String): List<Case>
}