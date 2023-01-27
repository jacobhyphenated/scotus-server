package com.hyphenated.scotus.case

import com.hyphenated.scotus.search.SearchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty

@RestController
@RequestMapping("cases")
class CaseController(private val caseService: CaseService,
                     private val searchService: SearchService) {

  @GetMapping
  fun getAllCases() = caseService.getAllCases()

  @GetMapping("term/{termId}")
  fun getCasesByTerm(@PathVariable termId: Long) = caseService.getTermCases(termId)

  @GetMapping("{id}")
  fun getCaseById(@PathVariable id: Long): ResponseEntity<CaseResponse> {
    return caseService.getCase(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
  }

  @GetMapping("search/{searchTerm}")
  fun searchCases(@PathVariable searchTerm: String) = searchService.searchCases(searchTerm)

  @GetMapping("term/{termId}/summary")
  fun termSummary(@PathVariable termId: Long) = caseService.getTermSummary(termId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createCase(@Valid @RequestBody request: CreateCaseRequest): CaseResponse {
    return caseService.createCase(request).also {
      searchService.indexCase(it.id)
    }
  }

  @PatchMapping("{id}")
  fun editCase(@PathVariable id: Long, @Valid @RequestBody request: PatchCaseRequest): ResponseEntity<CaseResponse> {
    return caseService.editCase(id, request)
        ?.let {
          searchService.indexCase(it.id)
          ResponseEntity.ok(it)
        }
        ?: ResponseEntity.notFound().build()
  }

  @DeleteMapping("{id}/argumentDate")
  fun removeArgumentDate(@PathVariable id: Long):  ResponseEntity<CaseResponse> {
    return caseService.removeArgumentDate(id)?.let {
      ResponseEntity.ok(it)
    } ?: ResponseEntity.notFound().build()
  }

  @PutMapping("{id}/index")
  fun indexCase(@PathVariable id: Long) {
    searchService.indexCase(id)
  }

  @PutMapping("indexAll")
  fun indexAll() {
    searchService.indexAllCases()
  }

  @PutMapping("{caseId}/dockets/{docketId}")
  fun addDocket(@PathVariable caseId: Long, @PathVariable docketId: Long): CaseResponse {
    return caseService.assignDocket(caseId, docketId).also {
      searchService.indexCase(caseId)
    }
  }

  @DeleteMapping("{caseId}/dockets/{docketId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun removeDocket(@PathVariable caseId: Long, @PathVariable docketId: Long) {
    caseService.removeDocket(caseId, docketId)
    searchService.indexCase(caseId)
  }
}

data class CreateCaseRequest(
    @get:NotEmpty
    val case: String,

    @get:NotEmpty
    val shortSummary: String,

    @get:Min(1)
    val termId: Long,

    val important: Boolean,

    val docketIds: List<Long>,

    val alternateTitles: List<String> = listOf()
)

data class PatchCaseRequest (
  val case: String?,
  val shortSummary: String?,
  val resultStatus: String?,
  val argumentDate: LocalDate?,
  val sitting: String?,
  val decisionDate: LocalDate?,
  val result: String?,
  val decisionSummary: String?,
  val decisionLink: String?,
  val termId: Long?,
  val important: Boolean?,
  val alternateTitles: List<String>?
)