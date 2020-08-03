package com.hyphenated.scotus.case

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.opinion.Opinion
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.websocket.server.PathParam

@RestController
@RequestMapping("cases")
class CaseController(private val caseService: CaseService) {

  @GetMapping
  fun getAllCases() = caseService.getAllCases()

  @GetMapping("term/{termId}")
  fun getCasesByTerm(@PathVariable termId: Long) = caseService.getTermCases(termId)

  @GetMapping("{id}")
  fun getCaseById(@PathVariable id: Long): ResponseEntity<CaseResponse> {
    return caseService.getCase(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
  }

  @GetMapping("term")
  fun getTerms() = caseService.getAllTerms()

  @GetMapping("title/{title}")
  fun searchByCaseTitle(@PathVariable title: String) = caseService.searchByCaseTitle(title)

  @PostMapping("term")
  @ResponseStatus(HttpStatus.CREATED)
  fun createTerm(@Valid @RequestBody request: CreateTermRequest): Term {
    return caseService.createTerm(request.name, request.otName)
  }

  @GetMapping("term/{termId}/summary")
  fun termSummary(@PathVariable termId: Long) = caseService.getTermSummary(termId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createCase(@Valid @RequestBody request: CreateCaseRequest): CaseResponse {
    return caseService.createCase(request)
  }

  @PatchMapping("{id}")
  fun editCase(@PathVariable id: Long, @Valid @RequestBody request: PatchCaseRequest): ResponseEntity<CaseResponse> {
    return caseService.editCase(id, request)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
  }

  @PutMapping("{caseId}/dockets/{docketId}")
  fun addDocket(@PathVariable caseId: Long, @PathVariable docketId: Long): CaseResponse {
    return caseService.assignDocket(caseId, docketId)
  }

  @DeleteMapping("{caseId}/dockets/{docketId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun removeDocket(@PathVariable caseId: Long, @PathVariable docketId: Long) {
    caseService.removeDocket(caseId, docketId)
  }
}

data class CreateCaseRequest(
    @get:NotEmpty
    val case: String,

    @get:NotEmpty
    val shortSummary: String,

    @get:NotEmpty
    val status: String,

    @get:Min(1)
    val termId: Long,

    val important: Boolean,

    val docketIds: List<Long>
)

data class PatchCaseRequest (
  val case: String?,
  val shortSummary: String?,
  val status: String?,
  val argumentDate: LocalDate?,
  val decisionDate: LocalDate?,
  val result: String?,
  val decisionSummary: String?,
  val termId: Long?,
  val important: Boolean?
)

data class CreateTermRequest(
    @get:NotEmpty
    val name: String,
    @get:NotEmpty
    val otName: String
)