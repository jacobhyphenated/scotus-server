package com.hyphenated.scotus.justice

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("justices")
class JusticeController(private val repo: JusticeRepo) {

  @GetMapping("")
  fun getAllJustices(): List<Justice> {
    return repo.findAll()
  }

  @GetMapping("active")
  fun getActiveJustices(): List<Justice> {
    return repo.findActive()
  }
}