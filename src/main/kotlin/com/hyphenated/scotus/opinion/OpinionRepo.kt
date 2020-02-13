package com.hyphenated.scotus.opinion

import org.springframework.data.jpa.repository.JpaRepository

interface OpinionRepo: JpaRepository<Opinion, Long> {
}