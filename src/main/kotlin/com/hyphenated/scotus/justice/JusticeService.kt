package com.hyphenated.scotus.justice

import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class JusticeService(private val justiceRepo: JusticeRepo) {

  fun findAll(): List<Justice> = justiceRepo.findAll()

  fun findActive(): List<Justice> = justiceRepo.findByDateRetiredIsNull()

  fun findByName(name: String): List<Justice> = justiceRepo.findByNameIgnoreCaseContaining(name)

  fun findById(id: Long): Justice? = justiceRepo.findByIdOrNull(id)

  @PreAuthorize("hasRole('ADMIN')")
  fun createJustice(justice: Justice): Justice {
    if (justice.id != null) {
      throw JusticeCreateWithIdException()
    }
    return justiceRepo.save(justice)
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun retireJustice(id: Long, retireDate: LocalDate): Justice? {
    val justice =  justiceRepo.findByIdOrNull(id) ?: return null
    val updated = Justice(justice.id, justice.name, justice.dateConfirmed, justice.birthday, retireDate)
    return justiceRepo.save(updated)
  }
}