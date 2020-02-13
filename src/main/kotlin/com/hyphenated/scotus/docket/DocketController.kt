package com.hyphenated.scotus.docket

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("dockets")
class DocketController(private val docketService: DocketService) {

  @GetMapping("")
  fun getAllDockets(): List<DocketResponse> {
    return docketService.findAll()
  }

  @GetMapping("case/{caseId}")
  fun getDocketsByCase(@PathVariable caseId: Long): List<DocketResponse> {
    return docketService.findByCaseId(caseId)
  }

  @GetMapping("{id}")
  fun getById(@PathVariable id: Long): ResponseEntity<Docket> {
    return docketService.findById(id)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
  }
}