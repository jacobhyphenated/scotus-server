package com.hyphenated.scotus.case

import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TermSummaryResponse (
  val termId: Long,
  val termEndDate: LocalDate?,
  val justiceSummary: List<TermJusticeSummary>,
  val courtSummary: List<TermCourtSummary>,
  var justiceAgreement: List<JusticeAgreementResponse>,
  val unanimous: List<Case>,
  val partySplit: List<Case>,
  val averageDecisionDays: Int,
  val medianDecisionDays: Int
)

class TermJusticeSummary (
  val justice: Justice,
  var majorityAuthor: Int = 0,
  var concurringAuthor: Int = 0,
  var concurJudgementAuthor: Int = 0,
  var dissentAuthor: Int = 0,
  var dissentJudgementAuthor: Int = 0,
  var casesInMajority: Int = 0,
  var casesWithOpinion: Int = 0
) {

  val percentInMajority: Float
    get() = casesInMajority.toFloat() / casesWithOpinion

  fun incrementType(opinionType: OpinionType) {
    when(opinionType) {
      OpinionType.MAJORITY -> majorityAuthor++
      OpinionType.CONCURRENCE -> concurringAuthor++
      OpinionType.CONCUR_JUDGEMENT -> concurJudgementAuthor++
      OpinionType.DISSENT -> dissentAuthor++
      OpinionType.DISSENT_JUDGEMENT -> dissentJudgementAuthor++
      // Per Curium Opinions don't have official authors, so are not counted
      OpinionType.PER_CURIUM -> {}
    }
  }
}

class TermCourtSummary (
  val court: Court,
  var cases: Int = 0,
  var affirmed: Int = 0,
  var reversedRemanded: Int = 0
)

class JusticeAgreementResponse(
  val justiceId: Long,
  val opinionAgreementMap: Map<Long, Float>,
  val caseAgreementMap: Map<Long, Float>
)

class JusticeAgreementMap(
  private val justiceId: Long,
  private var totalOpinions: Int = 0,
  private var totalCases: Int = 0,
  private val opinionAgreementMap: MutableMap<Long, Int> = mutableMapOf(),
  private val caseAgreementMap: MutableMap<Long, Int> = mutableMapOf()
) {

  fun countAgreementFromCase(case: Case) {
    val majorityJustices = case.opinions.filter { o ->  o.opinionType in CaseService.MAJORITY_TYPES }
      .flatMap { o -> o.opinionJustices.map { oj -> oj.justice.id }}
      .filterNotNull()
      .toSet()

    if (justiceId in majorityJustices) {
      majorityJustices.forEach{ caseAgreementMap.incrementOrPut(it) }
      totalCases++
      return
    }

    val dissentingJustices = case.opinions.filter { o ->  o.opinionType in CaseService.DISSENT_TYPES }
      .flatMap { o -> o.opinionJustices.map { oj -> oj.justice.id }}
      .filterNotNull()
      .toSet()

    if (majorityJustices.size + dissentingJustices.size != 9) {
      log.debug("Case ${case.id} does not have 9 total justices")
    }

    if (justiceId in dissentingJustices) {
      dissentingJustices.forEach{ caseAgreementMap.incrementOrPut(it) }
      totalCases++
    }
  }

  fun countAgreementFromOpinion(opinion: Opinion) {
    val justices = opinion.opinionJustices.mapNotNull { it.justice.id }
    if (justiceId in justices) {
      totalOpinions++
      justices.forEach { opinionAgreementMap.incrementOrPut(it) }
    }
  }
  fun toResponse(): JusticeAgreementResponse {
    val opinionResultMap = opinionAgreementMap.map { (justiceId, caseCount) ->
      justiceId to caseCount.toFloat() / totalOpinions
    }.toMap()
    val caseResultMap = caseAgreementMap.map { (justiceId, caseCount) ->
      justiceId to caseCount.toFloat() / totalCases
    }.toMap()
    return JusticeAgreementResponse(justiceId, opinionResultMap, caseResultMap)
  }

  companion object {
    private val log = LoggerFactory.getLogger(TermSummaryResponse::class.java)
  }
}

fun <T>MutableMap<T, Int>.incrementOrPut(key: T) {
  val current = this.getOrPut(key) { 0 }
  this[key] = current + 1
}
