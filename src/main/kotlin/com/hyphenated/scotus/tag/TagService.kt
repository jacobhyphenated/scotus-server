package com.hyphenated.scotus.tag

import com.hyphenated.scotus.docket.TagNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TagService(private val tagRepo: TagRepo) {

  @Transactional
  fun getAllTags(): List<Tag> {
    return tagRepo.findAll()
  }

  @Transactional
  fun getCasesForTag(tagId: Long): TagDetailResponse {
    val tag = tagRepo.findByIdOrNull(tagId) ?: throw TagNotFoundException(tagId)
    return TagDetailResponse(tagId, tag.name, tag.description, tag.cases.map { it })
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createTag(request: CreateTagRequest): Tag {
    val tag = Tag(null, request.name, request.description, emptyList())
    return tagRepo.save(tag)
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun editTag(tagId: Long, request: EditTagRequest): Tag {
    val tag = tagRepo.findByIdOrNull(tagId) ?: throw TagNotFoundException(tagId)
    val updated = tag.copy(
      name = request.name ?: tag.name,
      description = request.description ?: tag.description
    )
    return tagRepo.save(updated)
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun deleteTag(tagId: Long) {
    val tag = tagRepo.findByIdOrNull(tagId) ?: return
    if (tag.cases.isNotEmpty()) {
      throw TagDeleteConstraintException(tagId)
    }
    tagRepo.delete(tag)
  }
}

class TagDeleteConstraintException(id: Long): RuntimeException("Cannot delete Tag with id $id. Tag is currently associated with cases.")
