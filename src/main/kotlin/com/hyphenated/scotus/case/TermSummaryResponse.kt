package com.hyphenated.scotus.case

import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionType
import java.time.LocalDate

class TermSummaryResponse (
  val termId: Long,
  val termEndDate: LocalDate?,
  val justiceSummary: List<TermJusticeSummary>,
  val courtSummary: List<TermCourtSummary>,
  var justiceAgreement: List<JusticeAgreementResponse>,
  val unanimous: List<Case>,
  val partySplit: List<Case>
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
  val agreementMap: Map<Long, Float>
)

class JusticeAgreementMap(
  private val justiceId: Long,
  private var totalCases: Int = 0,
  private val agreementMap: MutableMap<Long, Int> = mutableMapOf()
) {

  fun countAgreementFromOpinion(opinion: Opinion) {
    val justices = opinion.opinionJustices.mapNotNull { it.justice.id }
    if (justiceId in justices) {
      totalCases++
      justices.forEach { id ->
        val agreementNum = agreementMap.getOrPut(id) { 0 }
        agreementMap[id] = agreementNum + 1
      }
    }
  }
  fun toResponse(): JusticeAgreementResponse {
    val resultMap = agreementMap.map { (justiceId, caseCount) ->
      justiceId to caseCount.toFloat() / totalCases
    }.toMap()
    return JusticeAgreementResponse(justiceId, resultMap)
  }
}

