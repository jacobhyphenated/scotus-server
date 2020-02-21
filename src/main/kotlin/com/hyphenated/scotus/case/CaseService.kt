package com.hyphenated.scotus.case

import com.hyphenated.scotus.docket.CaseNotFoundException
import com.hyphenated.scotus.docket.DocketRepo
import com.hyphenated.scotus.docket.NoDocketIdException
import org.apache.commons.logging.Log
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import javax.sound.midi.Patch
import javax.transaction.Transactional

@Service
class CaseService(private val caseRepo: CaseRepo,
                  private val docketRepo: DocketRepo) {

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

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createCase(request: CreateCaseRequest): CaseResponse {
    val dockets = docketRepo.findAllById(request.docketIds)

    val newCase = caseRepo.save(Case(null, request.case, request.shortSummary, request.status, null, null, null,
        null, request.term, emptyList(), dockets))
    docketRepo.saveAll(dockets.map { it.copy(case = newCase) })
    return newCase.toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun editCase(id: Long, request: PatchCaseRequest): CaseResponse? {
    val case = caseRepo.findByIdOrNull(id) ?: return null
    val editCase = Case(
        case.id,
        request.case ?: case.case,
        request.shortSummary ?: case.shortSummary,
        request.status ?: case.status,
        request.argumentDate ?: case.argumentDate,
        request.decisionDate ?: case.decisionDate,
        request.result ?: case.result,
        request.decisionSummary ?: case.decisionSummary,
        request.term ?: case.term,
        case.opinions,
        case.dockets
    )
    return caseRepo.save(editCase).toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun assignDocket(caseId: Long, docketId: Long): CaseResponse {
    val case = caseRepo.findByIdOrNull(caseId) ?: throw CaseNotFoundException(caseId)
    if (case.dockets.map { it.id }.contains(docketId)) return case.toResponse()
    val docket = docketRepo.findByIdOrNull(docketId) ?: throw NoDocketIdException(docketId)
    if (docket.case != null) {
      throw DocketAlreadyAssignedException()
    }

    docketRepo.save(docket.copy(case = case))
    val newDocketList = case.dockets.toMutableList().apply { add(docket) }
    return caseRepo.save(case.copy(dockets = newDocketList)).toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun removeDocket(caseId: Long, docketId: Long) {
    val case = caseRepo.findByIdOrNull(caseId) ?: throw CaseNotFoundException(caseId)
    val docket = case.dockets.find { it.id == docketId } ?: return
    docketRepo.save(docket.copy(case = null))
    val newDocketList = case.dockets.toMutableList().apply { remove(docket) }
    caseRepo.save(case.copy(dockets = newDocketList))
  }

  companion object {
    private val log = LoggerFactory.getLogger(CaseService::class.java)
  }
}