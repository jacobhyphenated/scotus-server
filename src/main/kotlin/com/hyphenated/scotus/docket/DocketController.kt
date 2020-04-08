package com.hyphenated.scotus.docket

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

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

  @GetMapping("unassigned")
  fun getUnassignedDockets(): List<DocketResponse> {
    return docketService.findUnassigned()
  }

  @GetMapping("title/{title}")
  fun searchByTitle(@PathVariable title: String): List<DocketResponse> {
    return docketService.searchByTitle(title)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@Valid @RequestBody request: CreateDocketRequest): Docket {
    return docketService.createDocket(request)
  }

  @PatchMapping("{docketId}")
  fun update(@PathVariable docketId: Long, @RequestBody request: EditDocketRequest): Docket {
    return docketService.editDocket(docketId, request)
  }

}

data class CreateDocketRequest(
    @get:NotEmpty
    val title: String,
    @get:NotEmpty
    val docketNumber: String,
    val lowerCourtId: Long,
    val lowerCourtRuling: String,
    @get:NotEmpty
    val status: String
)

data class EditDocketRequest(
    val title: String?,
    val docketNumber: String?,
    val lowerCourtRuling: String?,
    val lowerCourtOverruled: Boolean?,
    val status: String?,
    val caseId: Long?
)