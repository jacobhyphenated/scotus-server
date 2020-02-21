package com.hyphenated.scotus.court

import com.hyphenated.scotus.docket.CourtNotFoundException
import com.hyphenated.scotus.docket.NoCourtIdException
import com.hyphenated.scotus.justice.CourtCreateWithIdException
import com.hyphenated.scotus.justice.Justice
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class CourtService(private val courtRepo: CourtRepo) {

  fun findAll(): List<Court> = courtRepo.findAll()

  fun findById(id: Long): Court? {
    return courtRepo.findByIdOrNull(id)
  }

  @PreAuthorize("hasRole('ADMIN')")
  fun createCourt(court: Court): Court {
    if (court.id != null) {
      throw CourtCreateWithIdException()
    }
    return courtRepo.save(court)
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  fun edit(id: Long, court: Court): Court {
    if (!courtRepo.existsById(id)) {
      throw CourtNotFoundException(id)
    }
    return courtRepo.save(Court(id, court.shortName, court.name))
  }

  @PreAuthorize("hasRole('ADMIN')")
  fun delete(id: Long) {
    try {
      courtRepo.deleteById(id)
    } catch (e: DataIntegrityViolationException) {
      throw CourtDeleteConstraintException(id)
    }
  }
}