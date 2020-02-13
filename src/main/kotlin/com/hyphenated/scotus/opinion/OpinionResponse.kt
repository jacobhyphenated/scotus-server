package com.hyphenated.scotus.opinion

class OpinionResponse (
    val OpinionId: Long,
    val opinionType: OpinionType,
    val summary: String,
    val justices: List<OpinionJusticeResponse>

)

class OpinionJusticeResponse(
    val justiceId: Long,
    val isAuthor: Boolean,
    val justiceName: String
)

fun Opinion.toResponse(): OpinionResponse {
  return OpinionResponse(this.id!!, this.opinionType, this.summary, this.opinionJustices.map { it.toResponse() })
}

fun OpinionJustice.toResponse(): OpinionJusticeResponse {
  return OpinionJusticeResponse(this.justice.id!!, this.isAuthor, this.justice.name)
}
