package com.hyphenated.scotus.case

import com.hyphenated.scotus.docket.DocketCaseResponse
import com.hyphenated.scotus.docket.toCaseResponse
import com.hyphenated.scotus.opinion.OpinionResponse
import com.hyphenated.scotus.opinion.toResponse
import java.time.LocalDate

class CaseResponse(
    val id: Long,
    val case: String,
    val shortSummary: String,
    val status: String,
    val argumentDate: LocalDate?,
    val decisionDate: LocalDate?,
    val result: String?,
    val decisionSummary: String?,
    val term: String,
    val opinions: List<OpinionResponse>,
    val dockets: List<DocketCaseResponse>
)

fun Case.toResponse(): CaseResponse {
  return CaseResponse(id!!, case, shortSummary, status, argumentDate, decisionDate, result, decisionSummary, term,
      opinions.map { it.toResponse() }, dockets.map { it.toCaseResponse() })
}