package com.hyphenated.scotus.opinion

import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.docket.NoCaseIdException
import com.hyphenated.scotus.docket.NoJusticeIdException
import com.hyphenated.scotus.docket.OpinionNotFoundException
import com.hyphenated.scotus.justice.JusticeRepo
import com.hyphenated.scotus.search.SearchService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional

@Service
class OpinionService(private val opinionRepo: OpinionRepo,
                     private val caseRepo: CaseRepo,
                     private val justiceRepo: JusticeRepo,
                     private val searchService: SearchService
) {

  @Transactional
  fun getByCaseId(caseId: Long): List<OpinionResponse> {
    return opinionRepo.findByCaseId(caseId).map { it.toResponse() }
  }

  @Transactional
  fun getById(id: Long): OpinionResponse? {
    return opinionRepo.findByIdOrNull(id)?.toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createOpinion(request: CreateOpinionRequest): OpinionResponse {
    val case = caseRepo.findByIdOrNull(request.caseId) ?: throw NoCaseIdException(request.caseId)

    val opinionJustices = mutableListOf<OpinionJustice>()
    val opinion = Opinion(null, case, request.opinionType, opinionJustices, request.summary)
    var hasAuthor = false
    request.justices.forEach {
      val justice = justiceRepo.findByIdOrNull(it.justiceId) ?: throw NoJusticeIdException(it.justiceId)
      opinionJustices.add(OpinionJustice(null, it.isAuthor, opinion, justice))
      hasAuthor = if (it.isAuthor && hasAuthor) throw MultipleOpinionAuthorException() else hasAuthor || it.isAuthor
    }
    if (!hasAuthor) {
      throw NoOpinionAuthorException()
    }
    searchService.indexCase(case.id!!)
    return opinionRepo.save(opinion).toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun deleteOpinion(id: Long) {
    val opinion = opinionRepo.findByIdOrNull(id) ?: return
    val caseId = opinion.case.id
    opinionRepo.delete(opinion)
    caseId?.apply { searchService.indexCase(this) }
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun editSummary(id: Long, summary: String): OpinionResponse {
    val opinion = opinionRepo.findByIdOrNull(id) ?: throw OpinionNotFoundException(id)
    val result = opinionRepo.save(opinion.copy(summary = summary)).toResponse()
    searchService.indexCase(result.caseId)
    return result
  }
}

class NoOpinionAuthorException: RuntimeException()
class MultipleOpinionAuthorException: RuntimeException()