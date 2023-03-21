package com.hyphenated.scotus.opinion

import com.hyphenated.scotus.justice.Justice
import jakarta.persistence.*

@Entity
@Table(name = "decision_justice")
data class OpinionJustice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "is_author")
    val isAuthor: Boolean,

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "opinion_id")
    val opinion: Opinion,

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "justice_id")
    val justice: Justice
)