package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.court.Court
import javax.persistence.*

@Entity
@Table
data class Docket (
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  val id: Long?,

  @ManyToOne(optional = true)
  @JoinColumn(name="case_id")
  val case: Case?,

  @Column
  val title: String,

  @Column(name = "docket_number")
  val docketNumber: String,

  @ManyToOne(optional = false)
  @JoinColumn(name = "lower_court_id")
  val lowerCourt: Court,

  @Column(name = "ruling")
  val lowerCourtRuling: String,

  @Column(name = "overruled")
  val lowerCourtOverruled: Boolean?,

  @Column
  val status: String // Listed/Relisted, Granted, DIG (dismissed as improvidently granted)?, remanded, declined, judgement_issued
)