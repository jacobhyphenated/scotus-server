package com.hyphenated.scotus.case.term

import org.springframework.data.jpa.repository.JpaRepository

interface TermRepo: JpaRepository<Term, Long> {
}