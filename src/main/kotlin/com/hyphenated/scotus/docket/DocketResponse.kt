package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.court.Court
import javax.persistence.*

class DocketCaseResponse (
    val docketId: Long,
    val docketNumber: String,
    val lowerCourt: Court,
    val lowerCourtOverruled: Boolean?
)

class DocketResponse (
    val id: Long,
    val caseId: Long?,
    val title: String,
    val docketNumber: String,
    val lowerCourtId: Long,
    val lowerCourtRuling: String,
    val lowerCourtOverruled: Boolean?,
    val status: String
)

fun Docket.toCaseResponse(): DocketCaseResponse {
  return DocketCaseResponse(id!!, docketNumber, lowerCourt, lowerCourtOverruled)
}

fun Docket.toResponse(): DocketResponse {
  return DocketResponse(id!!, case?.id, title, docketNumber, lowerCourt.id!!, lowerCourtRuling, lowerCourtOverruled, status)
}