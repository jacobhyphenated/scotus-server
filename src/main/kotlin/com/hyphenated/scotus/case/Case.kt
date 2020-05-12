package com.hyphenated.scotus.case

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.opinion.Opinion
import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "scotus_case")
data class Case (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "case_title")
    val case: String,

    @Column(name = "short_summary")
    val shortSummary: String,

    @Column
    val status: String,

    @Column(name = "argument_date", nullable = true)
    val argumentDate: LocalDate?,

    @Column(name = "decision_date", nullable = true)
    val decisionDate: LocalDate?,

    @Column(nullable = true)
    val result: String?,

    @Column(name = "decision_summary_short", nullable = true)
    val decisionSummary: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "termId")
    val term: Term,

    @Column
    val important: Boolean,

    @get:JsonIgnore
    @OneToMany(mappedBy = "case", fetch = FetchType.LAZY)
    val opinions: List<Opinion>,

    @get:JsonIgnore
    @OneToMany(mappedBy = "case", fetch = FetchType.LAZY)
    val dockets: List<Docket>
)