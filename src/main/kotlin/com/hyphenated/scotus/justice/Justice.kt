package com.hyphenated.scotus.justice

import java.time.LocalDate
import javax.persistence.*

@Table
@Entity
data class Justice (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long?,

    @Column
    val name: String,

    @Column(name = "date_confirmed")
    val dateConfirmed: LocalDate,

    @Column
    val birthday: LocalDate,

    @Column(name = "date_retired", nullable = true)
    val dateRetired: LocalDate?

)