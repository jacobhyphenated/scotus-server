package com.hyphenated.scotus.case

import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.OpinionType
import java.time.LocalDate

class TermSummaryResponse (
    val termId: Long,
    val termEndDate: LocalDate?,
    val justiceSummary: List<TermJusticeSummary>,
    val courtSummary: List<TermCourtSummary>
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

  @Suppress("NON_EXHAUSTIVE_WHEN")
  fun incrementType(opinionType: OpinionType) {
    when(opinionType) {
      OpinionType.MAJORITY -> majorityAuthor++
      OpinionType.CONCURRENCE -> concurringAuthor++
      OpinionType.CONCUR_JUDGEMENT -> concurJudgementAuthor++
      OpinionType.DISSENT -> dissentAuthor++
      OpinionType.DISSENT_JUDGEMENT -> dissentJudgementAuthor++
      // Per Curium Opinions don't have official authors, so are not counted
    }
  }
}

class TermCourtSummary (
    val court: Court,
    var cases: Int = 0,
    var affirmed: Int = 0,
    var reversedRemanded: Int = 0
)