package com.hyphenated.scotus.court

import javax.persistence.*
import javax.validation.constraints.NotEmpty

@Entity
@Table
data class Court(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @get:NotEmpty
    @Column(name = "short_name")
    val shortName: String,

    @get:NotEmpty
    @Column
    val name: String
)