package com.hyphenated.scotus.case

import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.docket.*
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.OpinionType
import com.hyphenated.scotus.tag.TagRepo
import com.hyphenated.scotus.term.TermRepo
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.LocalDate
import jakarta.transaction.Transactional
import java.time.temporal.ChronoUnit

@Service
class CaseService(private val caseRepo: CaseRepo,
                  private val docketRepo: DocketRepo,
                  private val decisionLinkClient: CaseURLValidationClient,
                  private val termRepo: TermRepo,
                  private val tagRepo: TagRepo) {

  fun getAllCases(): List<Case> {
    return caseRepo.findAll()
  }

  fun getTermCases(termId: Long): List<Case> {
    return caseRepo.findByTermId(termId)
  }

  @Transactional
  fun getTermSummary(termId: Long): TermSummaryResponse {
    val termCases = caseRepo.findByTermId(termId)
    if (termCases.isEmpty()) {
      throw NoTermIdException(termId)
    }
    val justiceSummary = mutableListOf<TermJusticeSummary>()
    val courtSummary = mutableListOf<TermCourtSummary>()
    val unanimousCases = mutableListOf<Case>()
    val partySplitCases = mutableListOf<Case>()
    val justiceAgreementMap = mutableListOf<JusticeAgreementMap>()
    var lastCaseDate: LocalDate? = null
    val decisionDays = mutableListOf<Int>()
    val meritCases = termCases.filter { it.opinions.isNotEmpty() && it.argumentDate != null }
    meritCases.forEach {
      if (it.decisionDate?.isAfter(lastCaseDate ?: LocalDate.MIN) == true) {
        lastCaseDate = it.decisionDate
      }
      this.evaluateOpinionAuthorSummary(it, justiceSummary)
      this.evaluateCourtSummary(it, courtSummary)
      if (isUnanimous(it)) {
        unanimousCases.add(it)
      } else if (isSplitOnParty(it)) {
        partySplitCases.add(it)
      }
      // Merit cases should have a decision date. Catch and log if it doesn't
      it.decisionDate?.let { decisionDate ->
        val daysToDecision = ChronoUnit.DAYS.between(it.argumentDate, decisionDate).toInt()
        decisionDays.add(daysToDecision)
      } ?: log.warn("No decision date for ${it.case} (${it.id})")
    }
    justiceSummary.mapNotNull { j -> j.justice.id }
      .forEach{ justiceId -> justiceAgreementMap.add(JusticeAgreementMap(justiceId)) }
    meritCases.forEach { this.evaluateJusticeAgreement(it, justiceAgreementMap) }
    val averageDecisionDays = decisionDays.average().toInt()
    val medianDecisionDays = decisionDays.sorted().let {
      if (it.size % 2 == 0) {
        (it[it.size / 2] + it[(it.size - 1) / 2]) / 2
      }
      else {
        it[it.size / 2]
      }
    }
    return TermSummaryResponse(termId, lastCaseDate, justiceSummary, courtSummary,
      justiceAgreementMap.map { it.toResponse() }, unanimousCases, partySplitCases, averageDecisionDays, medianDecisionDays)
  }

  @Transactional
  fun getCase(id: Long): CaseResponse? {
    log.debug("get case called with $id")
    return caseRepo.findByIdOrNull(id)?.let {
      val archiveDecisionLink = decisionLinkClient.getValidCaseLink(it)
      it.copy(decisionLink = archiveDecisionLink).toResponse()
    }
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun createCase(request: CreateCaseRequest): CaseResponse {
    log.debug("create case called for ${request.case}")
    val dockets = docketRepo.findAllById(request.docketIds)
    val term = termRepo.findByIdOrNull(request.termId) ?: throw NoTermIdException(request.termId)

    var newCase = caseRepo.save(Case(null, request.case, listOf(), request.shortSummary, null, null,
      null, null, null, null, null, term, request.important, emptyList(), dockets, emptyList()
    ))
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
    val tags = request.tagIds?.map { tagRepo.findByIdOrNull(it) ?: throw NoTagIdException(it) }
    val editCase = Case(
        case.id,
        request.case ?: case.case,
        request.alternateTitles?.map { AlternateCaseTitle(null, case, it) } ?: case.alternateTitles,
        request.shortSummary ?: case.shortSummary,
        request.resultStatus ?: case.resultStatus,
        request.argumentDate ?: case.argumentDate,
        request.sitting ?: case.sitting,
        request.decisionDate ?: case.decisionDate,
        request.decisionLink ?: case.decisionLink,
        request.result ?: case.result,
        request.decisionSummary ?: case.decisionSummary,
        term ?: case.term,
        request.important ?: case.important,
        case.opinions,
        case.dockets,
        tags ?: case.tags,
    )
    return caseRepo.save(editCase).toResponse()
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun removeArgumentDate(id: Long): CaseResponse? {
    val case = caseRepo.findByIdOrNull(id) ?: return null
    val updatedCase = case.copy(argumentDate = null, sitting = null)
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

  fun isUnanimous(case: Case): Boolean {
    return case.opinions.all { it.opinionType !in DISSENT_TYPES }
  }

  fun isSplitOnParty(case: Case): Boolean {
    // TODO: Per curium? currently included in majority types so possible to be included as a split on party case
    val justicePartyInMajority = case.opinions.filter { it.opinionType in MAJORITY_TYPES }
      .flatMap { it.opinionJustices.map { oj -> oj.justice.party } }
      .toSet()

    val justicePartyInDissent = case.opinions.filter { it.opinionType in DISSENT_TYPES }
      .flatMap { it.opinionJustices.map { oj -> oj.justice.party } }
      .toSet()

    return justicePartyInMajority.size == 1
        && justicePartyInDissent.size == 1
        && justicePartyInMajority.intersect(justicePartyInDissent).isEmpty()
  }

  private fun evaluateOpinionAuthorSummary(case: Case, justiceSummary: MutableList<TermJusticeSummary>) {
    case.opinions.map {
      JusticeOpinionAuthorType(it.opinionJustices.first { oj -> oj.isAuthor }.justice, it.opinionType)
    }.toSet()
    .forEach { author -> retrieveJusticeSummary(justiceSummary, author.justice).incrementType(author.opinionType) }

    case.opinions.filter { o ->  o.opinionType in MAJORITY_TYPES }
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

  private fun evaluateJusticeAgreement(case: Case, justiceAgreementMap: MutableList<JusticeAgreementMap>) {
    case.opinions.forEach { opinion ->
      justiceAgreementMap.forEach { it.countAgreementFromOpinion(opinion) }
    }
    justiceAgreementMap.forEach { it.countAgreementFromCase(case) }
  }

  companion object {
    private val log = LoggerFactory.getLogger(CaseService::class.java)
    val MAJORITY_TYPES = listOf(OpinionType.MAJORITY, OpinionType.CONCUR_JUDGEMENT, OpinionType.CONCURRENCE, OpinionType.PER_CURIUM)
    val DISSENT_TYPES =  listOf(OpinionType.DISSENT, OpinionType.DISSENT_JUDGEMENT)
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