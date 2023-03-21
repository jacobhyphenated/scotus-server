package com.hyphenated.scotus.term

import jakarta.persistence.*

@Entity
@Table
data class Term (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column
    val name: String,

    @Column(name = "ot_name")
    val otName: String,

    @Column
    val inactive: Boolean = false
)