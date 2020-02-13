package com.hyphenated.scotus.case

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("cases")
class CaseController(private val caseService: CaseService) {

  @GetMapping
  fun getAllCases() = caseService.getAllCases()

  @GetMapping("term")
  fun getCasesByTerm(@RequestParam term: String) = caseService.getTermCases(term)

  @GetMapping("{id}")
  fun getCaseById(@PathVariable id: Long): ResponseEntity<CaseResponse> {
    return caseService.getCase(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
  }
}