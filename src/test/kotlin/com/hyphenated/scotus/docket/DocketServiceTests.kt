package com.hyphenated.scotus.docket

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.court.CourtRepo
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
class DocketServiceTests {

  @Mock
  private lateinit var docketRepo: DocketRepo

  @Mock
  private lateinit var courtRepo: CourtRepo

  @Mock
  private lateinit var caseRepo: CaseRepo

  @InjectMocks
  private lateinit var docketService: DocketService

  private val lowerCourt = Court(1, "te01", "test court 1")
  private val case = Case(100, "New York v. California", listOf(), "East coast vs west coast showdown",
    "REMANDED", LocalDate.of(2019, 10, 1), LocalDate.of(2020, 1, 5),
    null, "7-2", "Remanded for further arguments on what constitutes the definition of pizza",
    Term(50, "2019-2020", "OT2019"), true, listOf(), listOf()
  )
  private val dockets = listOf(
    Docket(1, null, "ABC v. DEF", "01-222", lowerCourt, "2-1", null, "PENDING"),
    Docket(2, case, "New York v. California", "19-002", lowerCourt,
      "New york has superior public transportation", true, "DONE"),
    Docket(3, case, "California Pizza Kitchen v. Sbarros", "19-003", lowerCourt,
      "The best pizza is the longest pizza", true, "DONE")
  )

  @Test
  fun testFindAll() {
    whenever(docketRepo.findAll()).thenReturn(dockets)
    val result = docketService.findAll()
    assertThat(result).hasSize(3)
  }

  @Test
  fun testFindByCaseId() {
    whenever(docketRepo.findByCaseId(100)).thenReturn(listOf(dockets[1], dockets[2]))
    val result = docketService.findByCaseId(100)
    assertThat(result).hasSize(2)
    assertThat(result[0].caseId).isEqualTo(100)
    assertThat(result[0].id).isEqualTo(2)
    assertThat(result[0].lowerCourtId).isEqualTo(1)
  }

  @Test
  fun testFindById() {
    whenever(docketRepo.findById(3)).thenReturn(Optional.of(dockets[2]))
    val result = docketService.findById(3)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(3)
    assertThat(result?.case?.id).isEqualTo(100)
    assertThat(result?.lowerCourt?.id).isEqualTo(1)
  }

  @Test
  fun testFindUnassigned() {
    whenever(docketRepo.findByCaseIsNull()).thenReturn(listOf(dockets[0]))
    val result = docketService.findUnassigned()
    assertThat(result).hasSize(1)
  }

  @Test
  fun testSearchByTitle() {
    whenever(docketRepo.findByTitleIgnoreCaseContaining("abc v. def")).thenReturn(listOf(dockets[0]))
    val result = docketService.searchByTitle("abc v. def")
    assertThat(result).hasSize(1)
  }

  @Test
  fun testCreateDocket() {
    whenever(courtRepo.findById(1)).thenReturn(Optional.of(lowerCourt))
    whenever(docketRepo.save<Docket>(any())).thenAnswer {
      val docket = it.arguments[0] as Docket
      docket.copy(id = 10)
    }
    val request = CreateDocketRequest("NCAA v. NFL", "19-100", 1, "5-0", "PENDING")
    val result = docketService.createDocket(request)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.lowerCourt.shortName).isEqualTo("te01")
    assertThat(result.case).isNull()
    assertThat(result.docketNumber).isEqualTo("19-100")
    assertThat(result.title).isEqualTo("NCAA v. NFL")
    assertThat(result.lowerCourtRuling).isEqualTo("5-0")
  }

  @Test
  fun testCreateDocket_noCourt() {
    whenever(courtRepo.findById(any())).thenReturn(Optional.empty())
    val request = CreateDocketRequest("NCAA v. NFL", "19-100", 5, "5-0", "PENDING")
    assertThrows<NoCourtIdException> { docketService.createDocket(request) }
  }

  @Test
  fun testEditDocket_title() {
    whenever(docketRepo.findById(3)).thenReturn(Optional.of(dockets[2]))
    whenever(docketRepo.save<Docket>(any())).thenAnswer { it.arguments[0] }
    val request = EditDocketRequest(
      title = "California Pizza v. Sbarro",
      docketNumber = "19-004",
      null,
      null,
      null,
      null
    )
    val result = docketService.editDocket(3, request)
    assertThat(result.title).isEqualTo("California Pizza v. Sbarro")
    assertThat(result.docketNumber).isEqualTo("19-004")
    assertThat(result.lowerCourt.id).isEqualTo(1)
    assertThat(result.lowerCourtRuling).isEqualTo("The best pizza is the longest pizza")
    assertThat(result.lowerCourtOverruled).isTrue
    assertThat(result.status).isEqualTo("DONE")
    assertThat(result.case?.id).isEqualTo(100)
  }

  @Test
  fun testEditDocket_status() {
    whenever(docketRepo.findById(3)).thenReturn(Optional.of(dockets[2]))
    whenever(docketRepo.save<Docket>(any())).thenAnswer { it.arguments[0] }
    val request = EditDocketRequest(
      null,
      null,
      "3-2",
      false,
      "COMPLETE",
      null
    )
    val result = docketService.editDocket(3, request)
    assertThat(result.title).isEqualTo("California Pizza Kitchen v. Sbarros")
    assertThat(result.docketNumber).isEqualTo("19-003")
    assertThat(result.lowerCourt.id).isEqualTo(1)
    assertThat(result.lowerCourtRuling).isEqualTo("3-2")
    assertThat(result.lowerCourtOverruled).isFalse
    assertThat(result.status).isEqualTo("COMPLETE")
    assertThat(result.case?.id).isEqualTo(100)
  }

  @Test
  fun testEditDocket_notFound() {
    whenever(docketRepo.findById(any())).thenReturn(Optional.empty())
    val request = EditDocketRequest("Test docket", null, null, null, null, null)
    assertThrows<DocketNotFoundException> { docketService.editDocket(10, request) }
  }

  @Test
  fun testEditDocket_noCaseFound() {
    whenever(docketRepo.findById(3)).thenReturn(Optional.of(dockets[2]))
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    val request = EditDocketRequest("Test docket", null, null, null, null, 2)
    assertThrows<NoCaseIdException> { docketService.editDocket(3, request) }
  }

  @Test
  fun testEditDocket_changeCase() {
    val case = Case(2, "California Pizza Kitchen v. Sbarros", listOf(),  "pizza showdown", "GRANTED",
      null, null, null, null, null, Term(50, "2019-2020", "OT2019"),
      true, listOf(), listOf()
    )
    whenever(docketRepo.findById(3)).thenReturn(Optional.of(dockets[2]))
    whenever(docketRepo.save<Docket>(any())).thenAnswer { it.arguments[0] }
    whenever(caseRepo.findById(2)).thenReturn(Optional.of(case))
    val request = EditDocketRequest(
      null,
      null,
      null,
      false,
      null,
      2
    )
    val result = docketService.editDocket(3, request)
    assertThat(result.case?.id).isEqualTo(2)
  }
}