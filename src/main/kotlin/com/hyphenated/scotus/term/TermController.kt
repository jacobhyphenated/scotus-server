package com.hyphenated.scotus.term

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

@RestController
@RequestMapping("terms")
class TermController (private val termService: TermService) {

  @GetMapping("")
  fun getTerms() = termService.getAllTerms()

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  fun createTerm(@Valid @RequestBody request: CreateTermRequest): Term {
    return termService.createTerm(request.name, request.otName, request.inactive)
  }

  @PatchMapping("{termId}")
  fun editTerm(@PathVariable termId: Long, @RequestBody request: EditTermRequest): ResponseEntity<Term> {
    return termService.editTerm(termId, request)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
  }

}

data class CreateTermRequest(
    @get:NotEmpty
    val name: String,
    @get:NotEmpty
    val otName: String,
    val inactive: Boolean = false
)

data class EditTermRequest(
    val name: String?,
    val otName: String?,
    val inactive: Boolean?
)
