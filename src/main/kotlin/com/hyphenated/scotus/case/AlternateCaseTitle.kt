package com.hyphenated.scotus.case

import jakarta.persistence.*

@Entity
@Table(name = "case_title")
data class AlternateCaseTitle(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "caseId")
  val case: Case,

  @Column
  val title: String
)
