package com.hyphenated.scotus.court

import javax.persistence.*

@Entity
@Table
data class Court(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long?,

    @Column(name = "short_name")
    val shortName: String,

    @Column
    val name: String
)