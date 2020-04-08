package com.hyphenated.scotus.docket

import org.springframework.data.jpa.repository.JpaRepository

interface DocketRepo: JpaRepository<Docket, Long> {

  fun findByCaseId(id: Long): List<Docket>

  fun findByCaseIsNull(): List<Docket>

  fun findByTitleIgnoreCaseContaining(title: String): List<Docket>
}