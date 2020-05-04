package com.hyphenated.scotus.opinion

import com.hyphenated.scotus.case.Case
import javax.persistence.*

@Table
@Entity
data class Opinion (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @ManyToOne
    @JoinColumn(name = "case_id")
    val case: Case,

    @Enumerated(EnumType.STRING)
    @Column(name = "opinion_type")
    val opinionType: OpinionType,

    @OneToMany(mappedBy = "opinion", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val opinionJustices: List<OpinionJustice>,

    @Column
    val summary: String
)

enum class OpinionType {
  MAJORITY,
  PER_CURIUM,
  CONCURRENCE,
  DISSENT,
  CONCUR_JUDGEMENT,
  DISSENT_JUDGEMENT
}