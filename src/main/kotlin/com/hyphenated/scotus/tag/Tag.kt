package com.hyphenated.scotus.tag

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hyphenated.scotus.case.Case
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table
data class Tag(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long?,

  @Column
  val name: String,

  @Column
  val description: String,

  @ManyToMany(mappedBy = "tags")
  @get:JsonIgnore
  val cases: List<Case>
)
