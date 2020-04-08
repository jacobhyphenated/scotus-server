package com.hyphenated.scotus.case.term

import javax.persistence.*

@Entity
@Table
data class Term (

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long?,

    @Column
    val name: String,

    @Column(name = "ot_name")
    val otName: String
)