package com.hyphenated.scotus.opinion

data class OpinionResponse (
    val id: Long,
    val caseId: Long,
    val opinionType: OpinionType,
    val summary: String,
    val justices: List<OpinionJusticeResponse>
)

data class OpinionCaseResponse (
    val opinionId: Long,
    val opinionType: OpinionType,
    val summary: String,
    val justices: List<OpinionJusticeResponse>
)

data class OpinionJusticeResponse(
    val justiceId: Long,
    val isAuthor: Boolean,
    val justiceName: String
)

fun Opinion.toResponse(): OpinionResponse {
  return OpinionResponse(this.id!!, this.case.id!!, this.opinionType, this.summary, this.opinionJustices.map { it.toResponse() })
}

fun Opinion.toCaseResponse(): OpinionCaseResponse {
  return OpinionCaseResponse(this.id!!, this.opinionType, this.summary, this.opinionJustices.map { it.toResponse() })
}

fun OpinionJustice.toResponse(): OpinionJusticeResponse {
  return OpinionJusticeResponse(this.justice.id!!, this.isAuthor, this.justice.name)
}
