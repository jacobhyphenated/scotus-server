package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.court.CourtRepo
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class DocketService(private val docketRepo: DocketRepo,
                    private val courtRepo: CourtRepo,
                    private val caseRepo: CaseRepo) {

  fun findAll(): List<DocketResponse> {
    return docketRepo.findAll().map { it.toResponse() }
  }

  fun findByCaseId(caseId: Long): List<DocketResponse> {
    return docketRepo.findByCaseId(caseId).map { it.toResponse() }
  }

  fun findById(id: Long): Docket? {
    return docketRepo.findByIdOrNull(id)
  }

  fun findUnassigned(): List<DocketResponse> {
    return docketRepo.findByCaseIsNull().map { it.toResponse() }
  }

  fun searchByTitle(title: String): List<DocketResponse> {
    return docketRepo.findByTitleIgnoreCaseContaining(title).map { it.toResponse() }
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createDocket(request: CreateDocketRequest): Docket {
    val court = courtRepo.findByIdOrNull(request.lowerCourtId) ?: throw NoCourtIdException(request.lowerCourtId)
    return docketRepo.save(Docket(null, null, request.title, request.docketNumber, court, request.lowerCourtRuling, null, request.status))
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun editDocket(docketId: Long, request: EditDocketRequest): Docket {
    val docket = docketRepo.findByIdOrNull(docketId) ?: throw DocketNotFoundException(docketId)
    val editDocket = Docket(
        docket.id,
        request.caseId?.let { caseRepo.findByIdOrNull(request.caseId) ?: throw NoCaseIdException(request.caseId) } ?: docket.case,
        request.title ?: docket.title,
        request.docketNumber ?: docket.docketNumber,
        docket.lowerCourt,
        request.lowerCourtRuling ?: docket.lowerCourtRuling,
        request.lowerCourtOverruled ?: docket.lowerCourtOverruled,
        request.status ?: docket.status
    )
    return docketRepo.save(editDocket)
  }
}