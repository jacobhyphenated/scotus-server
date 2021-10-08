package com.hyphenated.scotus.term

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.assertj.core.api.Assertions.assertThat
import java.util.*

@ExtendWith(MockitoExtension::class)
class TermServiceTests {

  @Mock
  private lateinit var termRepo: TermRepo

  @InjectMocks
  private lateinit var termService: TermService

  private val terms = listOf(Term(1, "2019", "OT2019"), Term(2, "2020", "OT2020"))

  @Test
  fun testGetAllTerms() {
    whenever(termRepo.findAll()).thenReturn(terms)
    val result = termService.getAllTerms()
    assertThat(result).hasSize(2)
  }

  @Test
  fun testCreateTerm() {
    whenever(termRepo.save<Term>(any())).thenAnswer {
      val term = it.arguments[0] as Term
      term.copy(id = 10)
    }
    val result = termService.createTerm("2019", "ot2019", false)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.name).isEqualTo("2019")
    assertThat(result.otName).isEqualTo("ot2019")
    assertThat(result.inactive).isEqualTo(false)
  }

  @Test
  fun testEditTerm_noTerm() {
    whenever(termRepo.findById(any())).thenReturn(Optional.empty())
    val result = termService.editTerm(5, EditTermRequest(null, null, null))
    assertThat(result).isNull()
  }

  @Test
  fun testEditTerm_name() {
    whenever(termRepo.findById(2)).thenReturn(Optional.of(Term(2, "2010-2011", "OT2010", true)))
    whenever(termRepo.save<Term>(any())).thenAnswer { it.arguments[0] }
    val result = termService.editTerm(2, EditTermRequest("2011-2012", "OT2011", null))
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.name).isEqualTo("2011-2012")
    assertThat(result?.otName).isEqualTo("OT2011")
    assertThat(result?.inactive).isEqualTo(true)
  }

  @Test
  fun testEditTerm_inactive() {
    whenever(termRepo.findById(2)).thenReturn(Optional.of(Term(2, "2010-2011", "OT2010", true)))
    whenever(termRepo.save<Term>(any())).thenAnswer { it.arguments[0] }
    val result = termService.editTerm(2, EditTermRequest(null, null, false))
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.name).isEqualTo("2010-2011")
    assertThat(result?.otName).isEqualTo("OT2010")
    assertThat(result?.inactive).isEqualTo(false)
  }
}