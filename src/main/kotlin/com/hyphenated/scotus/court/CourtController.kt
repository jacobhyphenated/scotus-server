package com.hyphenated.scotus.court

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("courts")
class CourtController(private val courtRepo: CourtRepo) {

  @GetMapping("")
  fun getAllCourts(): List<Court> = courtRepo.findAll()
}