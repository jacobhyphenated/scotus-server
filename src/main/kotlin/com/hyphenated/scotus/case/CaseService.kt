package com.hyphenated.scotus.case

import org.apache.commons.logging.Log
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class CaseService(private val caseRepo: CaseRepo) {

  fun getAllCases(): List<Case> {
    return caseRepo.findAll()
  }

  fun getTermCases(term: String): List<Case> {
    return caseRepo.findByTerm(term)
  }

  @Transactional
  fun getCase(id: Long): CaseResponse? {
    log.info("get case called with $id")
    return caseRepo.findByIdOrNull(id)?.toResponse()
  }

  companion object {
    private val log = LoggerFactory.getLogger(CaseService::class.java)
  }
}