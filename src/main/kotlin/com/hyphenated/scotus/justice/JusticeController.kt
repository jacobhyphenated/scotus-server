package com.hyphenated.scotus.justice

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import jakarta.transaction.Transactional
import jakarta.validation.Valid

@RestController
@RequestMapping("justices")
class JusticeController(private val service: JusticeService) {

  @GetMapping("")
  fun getAllJustices(): List<Justice> {
    return service.findAll()
  }

  @GetMapping("active")
  fun getActiveJustices(): List<Justice> {
    return service.findActive()
  }

  @GetMapping("{id}")
  fun findById(@PathVariable id: Long): ResponseEntity<Justice> {
    return service.findById(id)
        ?.let {ResponseEntity.ok (it)}
        ?: ResponseEntity.notFound().build()
  }

  @GetMapping("name/{justiceName}")
  fun getJusticeByName(@PathVariable justiceName: String): List<Justice> {
    return service.findByName(justiceName)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createJustice(@Valid @RequestBody justice: Justice): Justice {
    return service.createJustice(justice)
  }

  @PutMapping("{id}/retire")
  fun retireJustice(@PathVariable id: Long, @RequestBody body: JusticeRetireRequest): ResponseEntity<Justice> {
    return service.retireJustice(id, body.retireDate)
        ?.let { ResponseEntity.ok(it)}
        ?: ResponseEntity.notFound().build()
  }
}

class JusticeRetireRequest(
    val retireDate: LocalDate
)