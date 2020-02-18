package com.hyphenated.scotus.court

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("courts")
class CourtController(private val courtService: CourtService) {

  @GetMapping("")
  fun getAllCourts(): List<Court> = courtService.findAll()

  @GetMapping("{id}")
  fun getCourtById(@PathVariable id: Long): ResponseEntity<Court> {
    return  courtService.findById(id)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
  }

  @PostMapping
  fun createCourt(@Valid @RequestBody court: Court): Court {
    return courtService.createCourt(court)
  }

  @PutMapping("{id}")
  fun editCourt(@PathVariable id: Long, @Valid @RequestBody court: Court): Court {
    return courtService.edit(id, court)
  }

  @DeleteMapping("{id}")
  fun deleteCourt(@PathVariable id: Long) {
    return courtService.delete(id)
  }
}