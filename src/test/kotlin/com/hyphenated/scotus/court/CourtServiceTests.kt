package com.hyphenated.scotus.court

import com.hyphenated.scotus.docket.CourtNotFoundException
import com.hyphenated.scotus.justice.CourtCreateWithIdException
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.util.*

@ExtendWith(MockitoExtension::class)
class CourtServiceTests {

  @Mock
  private lateinit var courtRepo: CourtRepo

  @InjectMocks
  private lateinit var courtService: CourtService

  @Test
  fun testFindAll() {
    val courts = listOf(
      Court(1, "te01", "test court 1"),
      Court(2, "te02", "test court 2")
    )
    whenever(courtRepo.findAll()).thenReturn(courts)

    val result = courtService.findAll()
    assertThat(result).hasSize(2)
  }

  @Test
  fun testFindById() {
    // findByIdOrNull is a kotlin extension function and cannot be mocked by Mockito
    // instead mock the underlying findById function with optionals
    val court = Optional.of(Court(5, "te01", "test court 1"))
    whenever(courtRepo.findById(5)).thenReturn(court)
    val result = courtService.findById(5)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(5)
  }

  @Test
  fun testCreateCourt() {
    val newCourt = Court(null, "te03", "test court 3")
    whenever(courtRepo.save<Court>(any())).thenAnswer {
      var court = it.arguments[0] as Court
      court.copy(id = 10)
    }

    val result = courtService.createCourt(newCourt)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.name).isEqualTo("test court 3")
  }

  @Test
  fun testCreateCourt_NoId() {
    val newCourt = Court(10, "te03", "test court 3")
    assertThrows<CourtCreateWithIdException>{
      courtService.createCourt(newCourt)
    }
  }

  @Test
  fun testEditCourt() {
    whenever(courtRepo.existsById(10)).thenReturn(true)
    val editCourt = Court(10, "te04", "test court 4")
    whenever(courtRepo.save<Court>(any())).thenReturn(editCourt)

    val result = courtService.edit(10, editCourt)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.shortName).isEqualTo("te04")

    val args = argumentCaptor<Court>()
    verify(courtRepo).save(args.capture())
    assertThat(args.firstValue.id).isEqualTo(10)
    assertThat(args.firstValue.shortName).isEqualTo(editCourt.shortName)
    assertThat(args.firstValue.name).isEqualTo(editCourt.name)
  }

  @Test
  fun testEditCourt_doesNotExist() {
    whenever(courtRepo.existsById(4)).thenReturn(false)
    assertThrows<CourtNotFoundException> {
      courtService.edit(4, Court(null, "abc", "abcdef"))
    }
  }
  @Test
  fun testDeleteCourt() {
    courtService.delete(10)

    val args = argumentCaptor<Long>()
    verify(courtRepo).deleteById(args.capture())
    assertThat(args.firstValue).isEqualTo(10)
  }

  @Test
  fun testDeleteCourt_constraintViolation() {
    whenever(courtRepo.deleteById(10)).thenThrow(DataIntegrityViolationException(""))
    assertThrows<CourtDeleteConstraintException> {
      courtService.delete(10)
    }
  }
}
