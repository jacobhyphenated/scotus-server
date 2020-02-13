package com.hyphenated.scotus.opinion

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("opinions")
class OpinionController(private val opinionRepo: OpinionRepo) {

  @GetMapping
  fun getAllDecisions(): List<OpinionResponse> {
    return opinionRepo.findAll().map { it.toResponse() };
  }
}