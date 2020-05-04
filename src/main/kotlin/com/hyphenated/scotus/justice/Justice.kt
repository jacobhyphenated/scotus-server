package com.hyphenated.scotus.justice

import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotEmpty

@Table
@Entity
data class Justice (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @get:NotEmpty
    @Column
    val name: String,

    @Column(name = "date_confirmed")
    val dateConfirmed: LocalDate,

    @Column
    val birthday: LocalDate,

    @Column(name = "date_retired", nullable = true)
    val dateRetired: LocalDate?

)