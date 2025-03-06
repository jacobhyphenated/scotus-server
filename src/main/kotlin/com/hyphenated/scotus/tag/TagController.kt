package com.hyphenated.scotus.tag

import com.hyphenated.scotus.case.Case
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("tags")
class TagController(private val tagService: TagService) {

  @GetMapping("")
  fun getAllTags() = tagService.getAllTags()

  @GetMapping("{id}")
  fun getTagCases(@PathVariable id: Long): TagDetailResponse {
    return tagService.getCasesForTag(id)
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  fun createTag(@Valid @RequestBody request: CreateTagRequest): Tag {
    return tagService.createTag(request)
  }

  @PatchMapping("{id}")
  fun editTag(@Valid @RequestBody request: EditTagRequest, @PathVariable id: Long): Tag {
    return tagService.editTag(id, request)
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteTag(@PathVariable id: Long) {
    return tagService.deleteTag(id)
  }

}

data class TagDetailResponse(
  val id: Long,
  val name: String,
  val description: String,
  val cases: List<Case>
)

data class CreateTagRequest(
  @get:NotEmpty
  val name: String,
  @get:NotEmpty
  val description: String
)

data class EditTagRequest(
  val name: String?,
  val description: String?
)