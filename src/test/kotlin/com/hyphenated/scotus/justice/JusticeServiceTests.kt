package com.hyphenated.scotus.justice

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class JusticeServiceTests {

  @Mock
  private lateinit var justiceRepo:JusticeRepo

  @InjectMocks
  private lateinit var justiceService: JusticeService

  private val justices = listOf(
    Justice(1, "test1", LocalDate.of(1950, 1, 1), LocalDate.of(1990, 1,1), null),
    Justice(2, "test2", LocalDate.of(1952, 2, 2), LocalDate.of(1992, 2,2), null),
    Justice(3, "test3", LocalDate.of(1953, 3, 3), LocalDate.of(1993, 3,3), LocalDate.of(2013, 3,3)),
  )

  @Test
  fun testFindAll() {
    whenever(justiceRepo.findAll()).thenReturn(justices)
    val result = justiceService.findAll()
    assertThat(result).hasSize(3)
  }

  @Test
  fun testFindActive() {
    whenever(justiceRepo.findByDateRetiredIsNull()).thenReturn(listOf(justices[0], justices[1]))
    val result = justiceService.findActive()
    assertThat(result).hasSize(2)
  }

  @Test
  fun testFindByName() {
    whenever(justiceRepo.findByNameIgnoreCaseContaining("test3")).thenReturn(listOf(justices[2]))
    val result = justiceService.findByName("test3")
    assertThat(result).hasSize(1)
    assertThat(result[0].id).isEqualTo(3)
  }

  @Test
  fun testFindById() {
    whenever(justiceRepo.findById(2)).thenReturn(Optional.of(justices[1]))
    val result = justiceService.findById(2)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(2)
  }

  @Test
  fun testCreateJustice() {
    whenever(justiceRepo.save<Justice>(any())).thenAnswer {
      val justice = it.arguments[0] as Justice
      justice.copy(id = 10)
    }

    val request = Justice(null, "test10", LocalDate.of(1960, 10, 10), LocalDate.of(2000, 10, 10), null)
    val result = justiceService.createJustice(request)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.name).isEqualTo("test10")
  }

  @Test
  fun testCreateJustice_withId() {
    val request = Justice(3, "test10", LocalDate.of(1960, 10, 10), LocalDate.of(2000, 10, 10), null)
    assertThrows<JusticeCreateWithIdException> {
      justiceService.createJustice(request)
    }
  }

  @Test
  fun retireJustice() {
    whenever(justiceRepo.findById(1)).thenReturn(Optional.of(justices[0]))
    whenever(justiceRepo.save<Justice>(any())).thenAnswer { it.arguments[0] as Justice }

    val result = justiceService.retireJustice(1, LocalDate.of(2020,1, 1))
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(1)
    assertThat(result?.dateRetired).isEqualTo(LocalDate.of(2020,1, 1))
  }

  @Test
  fun retireJustice_doesNotExist() {
    whenever(justiceRepo.findById(11)).thenReturn(Optional.empty())
    val result = justiceService.retireJustice(11, LocalDate.of(2020, 2, 2))
    assertThat(result).isNull()
  }
}