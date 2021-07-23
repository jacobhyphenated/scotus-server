package com.hyphenated.scotus.case

import com.hyphenated.scotus.case.term.*
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.docket.CaseNotFoundException
import com.hyphenated.scotus.docket.DocketRepo
import com.hyphenated.scotus.docket.NoDocketIdException
import com.hyphenated.scotus.docket.NoTermIdException
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.OpinionType
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class CaseService(private val caseRepo: CaseRepo,
                  private val docketRepo: DocketRepo,
                  private val termRepo: TermRepo) {

  fun getAllCases(): List<Case> {
    return caseRepo.findAll()
  }

  fun getTermCases(termId: Long): List<Case> {
    return caseRepo.findByTermId(termId)
  }

  fun getAllTerms(): List<Term> {
    return termRepo.findAll()
  }

  @Transactional
  fun getTermSummary(termId: Long): TermSummaryResponse {
    val termCases = caseRepo.findByTermId(termId)
    if (termCases.isEmpty()) {
      throw NoTermIdException(termId)
    }
    val justiceSummary = mutableListOf<TermJusticeSummary>()
    val courtSummary = mutableListOf<TermCourtSummary>()
    var lastCaseDate: LocalDate? = null
    val casesWithOpinions = termCases.filter { it.opinions.isNotEmpty() }
    casesWithOpinions.forEach {
      if (it.decisionDate?.isAfter(lastCaseDate ?: LocalDate.MIN) == true) {
        lastCaseDate = it.decisionDate
      }
      this.evaluateOpinionAuthorSummary(it, justiceSummary)
      this.evaluateCourtSummary(it, courtSummary)
    }
    return TermSummaryResponse(termId, lastCaseDate, justiceSummary, courtSummary)
  }

  @Transactional
  fun getCase(id: Long): CaseResponse? {
    log.debug("get case called with $id")
    return caseRepo.findByIdOrNull(id)?.toResponse()
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

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createCase(request: CreateCaseRequest): CaseResponse {
    log.debug("create case called for ${request.case}")
    val dockets = docketRepo.findAllById(request.docketIds)
    val term = termRepo.findByIdOrNull(request.termId) ?: throw NoTermIdException(request.termId)

    var newCase = caseRepo.save(Case(null, request.case, listOf(), request.shortSummary, request.status, null,
      null, null, null, null, null, term, request.important, emptyList(), dockets))
    if (request.alternateTitles.isNotEmpty()) {
      val alternativeTitles = request.alternateTitles.map {
        AlternateCaseTitle(null, newCase, it)
      }
      newCase = caseRepo.save(newCase.copy(alternateTitles = alternativeTitles))
    }
    docketRepo.saveAll(dockets.map { it.copy(case = newCase) })
    return newCase.toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun editCase(id: Long, request: PatchCaseRequest): CaseResponse? {
    val case = caseRepo.findByIdOrNull(id) ?: return null
    val term = request.termId?.let { termRepo.findByIdOrNull(it) }
    val editCase = Case(
        case.id,
        request.case ?: case.case,
        request.alternateTitles?.map { AlternateCaseTitle(null, case, it) } ?: case.alternateTitles,
        request.shortSummary ?: case.shortSummary,
        request.status ?: case.status,
        request.argumentDate ?: case.argumentDate,
        request.sitting ?: case.sitting,
        request.decisionDate ?: case.decisionDate,
        request.decisionLink ?: case.decisionLink,
        request.result ?: case.result,
        request.decisionSummary ?: case.decisionSummary,
        term ?: case.term,
        request.important ?: case.important,
        case.opinions,
        case.dockets
    )
    return caseRepo.save(editCase).toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun removeArgumentDate(id: Long): CaseResponse? {
    val case = caseRepo.findByIdOrNull(id) ?: return null
    val updatedCase = case.copy(argumentDate = null)
    return caseRepo.save(updatedCase).toResponse()
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

  private fun evaluateOpinionAuthorSummary(case: Case, justiceSummary: MutableList<TermJusticeSummary>) {
    case.opinions.map {
      JusticeOpinionAuthorType(it.opinionJustices.first { oj -> oj.isAuthor }.justice, it.opinionType)
    }.toSet()
    .forEach { author -> retrieveJusticeSummary(justiceSummary, author.justice).incrementType(author.opinionType) }

    case.opinions.filter { o ->  o.opinionType == OpinionType.MAJORITY || o.opinionType == OpinionType.CONCUR_JUDGEMENT || o.opinionType == OpinionType.PER_CURIUM || o.opinionType == OpinionType.CONCURRENCE }
        .flatMap { o -> o.opinionJustices.map { oj -> oj.justice }}
        .toSet()
        .forEach { justice -> retrieveJusticeSummary(justiceSummary, justice).casesInMajority++ }

    case.opinions.flatMap { o -> o.opinionJustices.map { oj -> oj.justice } }
        .toSet()
        .forEach { justice -> retrieveJusticeSummary(justiceSummary, justice).casesWithOpinion++ }
  }

  private fun retrieveJusticeSummary(justiceSummary:MutableList<TermJusticeSummary>, justice: Justice): TermJusticeSummary {
    var summary = justiceSummary.find { it.justice.id == justice.id }
    if (summary == null) {
      summary = TermJusticeSummary(justice)
      justiceSummary.add(summary)
    }
    return summary
  }

  private fun evaluateCourtSummary(case: Case, courtSummary: MutableList<TermCourtSummary>) {
     case.dockets.filter { it.lowerCourtOverruled != null }
         .map { CourtOverturned(it.lowerCourt, it.lowerCourtOverruled!!) }
        .toSet()
        .forEach {
          var summary = courtSummary.find { cs -> cs.court.id === it.court.id }
          if (summary == null) {
            summary = TermCourtSummary(it.court)
            courtSummary.add(summary)
          }
          summary.cases++
          if (it.overturned) summary.reversedRemanded++ else summary.affirmed++
        }
  }

  companion object {
    private val log = LoggerFactory.getLogger(CaseService::class.java)
  }
}

private data class JusticeOpinionAuthorType(
    val justice: Justice,
    val opinionType: OpinionType
)

private data class CourtOverturned(
    val court: Court,
    val overturned: Boolean
)