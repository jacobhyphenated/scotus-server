package com.hyphenated.scotus.court

import org.springframework.data.jpa.repository.JpaRepository

interface CourtRepo: JpaRepository<Court, Long> {
}