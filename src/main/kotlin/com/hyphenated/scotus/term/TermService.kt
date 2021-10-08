package com.hyphenated.scotus.term

import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class TermService (private val termRepo: TermRepo) {

  fun getAllTerms(): List<Term> {
    return termRepo.findAll()
  }

  @PreAuthorize("hasRole('ADMIN')")
  fun createTerm(name: String, otName: String, inactive: Boolean): Term {
    return termRepo.save(Term(null, name, otName, inactive))
  }

  @PreAuthorize("hasRole('ADMIN')")
  fun editTerm(termId: Long, editTermRequest: EditTermRequest): Term? {
    val term = termRepo.findByIdOrNull(termId) ?: return null
    return termRepo.save(term.copy(
        name = editTermRequest.name ?: term.name,
        otName = editTermRequest.otName ?: term.otName,
        inactive = editTermRequest.inactive ?: term.inactive
    ))
  }
}
