package com.hyphenated.scotus.case

import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.docket.DocketCaseResponse
import com.hyphenated.scotus.docket.toCaseResponse
import com.hyphenated.scotus.opinion.OpinionResponse
import com.hyphenated.scotus.opinion.toResponse
import java.time.LocalDate

class CaseResponse(
    val id: Long,
    val case: String,
    val alternateTitles: List<String>,
    val shortSummary: String,
    val status: String,
    val argumentDate: LocalDate?,
    val decisionDate: LocalDate?,
    val result: String?,
    val decisionSummary: String?,
    val decisionLink: String?,
    val term: Term,
    val important: Boolean,
    val opinions: List<OpinionResponse>,
    val dockets: List<DocketCaseResponse>
)

fun Case.toResponse(): CaseResponse {
  return CaseResponse(id!!, case, alternateTitles.map { it.title }, shortSummary, status, argumentDate, decisionDate,
      result, decisionSummary, decisionLink, term, important, opinions.map { it.toResponse() }, dockets.map { it.toCaseResponse() })
}