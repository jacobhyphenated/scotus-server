package com.hyphenated.scotus.opinion

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

@RestController
@RequestMapping("opinions")
class OpinionController(private val opinionService: OpinionService) {

  @GetMapping("case/{caseId}")
  fun getByCase(@PathVariable caseId: Long): List<OpinionResponse> {
    return opinionService.getByCaseId(caseId)
  }

  @GetMapping("{id}")
  fun getById(@PathVariable id: Long): ResponseEntity<OpinionResponse> {
    return opinionService.getById(id)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createOpinion(@RequestBody @Valid request: CreateOpinionRequest): OpinionResponse {
    return opinionService.createOpinion(request)
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteOpinion(@PathVariable id: Long) {
    opinionService.deleteOpinion(id)
  }

  @PutMapping("{id}/summary")
  fun editSummary(@PathVariable id: Long, @Valid @RequestBody request: EditSummaryRequest): OpinionResponse {
    return opinionService.editSummary(id, request.summary)
  }
}

class CreateOpinionRequest(
    @get:Min(1)
    val caseId: Long,
    val opinionType: OpinionType,
    @get:NotEmpty
    val summary: String,
    @get:Valid
    @get:NotEmpty
    val justices: List<CreateOpinionJusticeRequest>
)

class CreateOpinionJusticeRequest(
    @get:Min(1)
    val justiceId: Long,
    val isAuthor: Boolean = false
)

class EditSummaryRequest(
    @get:NotEmpty
    val summary: String
)