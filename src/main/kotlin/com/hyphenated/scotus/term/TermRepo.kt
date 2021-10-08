package com.hyphenated.scotus.term

import org.springframework.data.jpa.repository.JpaRepository

interface TermRepo: JpaRepository<Term, Long> {
}