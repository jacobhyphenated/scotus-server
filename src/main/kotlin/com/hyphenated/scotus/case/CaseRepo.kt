package com.hyphenated.scotus.case

import org.springframework.data.jpa.repository.JpaRepository

interface CaseRepo: JpaRepository<Case, Long> {

  fun findByTerm(term: String): List<Case>
}