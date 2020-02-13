package com.hyphenated.scotus.docket

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class DocketService(private val docketRepo: DocketRepo) {
  fun findAll(): List<DocketResponse> {
    return docketRepo.findAll().map { it.toResponse() }
  }

  fun findByCaseId(caseId: Long): List<DocketResponse> {
    return docketRepo.findByCaseId(caseId).map { it.toResponse() }
  }

  fun findById(id: Long): Docket? {
    return docketRepo.findByIdOrNull(id)
  }
}