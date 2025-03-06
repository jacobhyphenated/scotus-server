package com.hyphenated.scotus.tag

import org.springframework.data.jpa.repository.JpaRepository

interface TagRepo : JpaRepository<Tag, Long> {
}