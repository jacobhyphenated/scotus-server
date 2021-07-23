package com.hyphenated.scotus.opinion

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.docket.NoCaseIdException
import com.hyphenated.scotus.docket.NoJusticeIdException
import com.hyphenated.scotus.docket.OpinionNotFoundException
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.justice.JusticeRepo
import com.hyphenated.scotus.search.SearchService
import com.nhaarman.mockitokotlin2.*
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
class OpinionServiceTests {

  @Mock
  private lateinit var opinionRepo: OpinionRepo

  @Mock
  private lateinit var caseRepo: CaseRepo

  @Mock
  private lateinit var justiceRepo: JusticeRepo

  @Mock
  private lateinit var searchService: SearchService

  @InjectMocks
  private lateinit var opinionService: OpinionService

  private val case = Case(100, "Apples v. Oranges", listOf(), "Judicial review of comparison techniques",
    "AFFIRMED", LocalDate.of(2020, 2,2), "February", LocalDate.of(2020, 6, 1), null,
    "7-2", "Apples cannot be successfully compared to oranges", Term(1, "2020", "ot2020"),
    false, listOf(), listOf())
  private val justices = listOf(
    Justice(1, "test justice 1", LocalDate.of(1990, 1, 1), LocalDate.of(1950,1,1), null),
    Justice(2, "test justice 2", LocalDate.of(1990, 1, 1), LocalDate.of(1950,1,1), null),
    Justice(3, "test justice 3", LocalDate.of(1990, 1, 1), LocalDate.of(1950,1,1), null),
    Justice(4, "test justice 4", LocalDate.of(1990, 1, 1), LocalDate.of(1950,1,1), null)
  )
  private val oj1s = mutableListOf<OpinionJustice>()
  private val oj2s = mutableListOf<OpinionJustice>()
  private val oj3s = mutableListOf<OpinionJustice>()
  private val opinions = listOf(
    Opinion(1, case, OpinionType.MAJORITY, oj1s, "It is not possible to compare apples and oranges"),
    Opinion(2, case, OpinionType.CONCURRENCE, oj2s, "This does not mean other types of fruit cannot be compared"),
    Opinion(3, case, OpinionType.DISSENT, oj3s, "Certain characteristics of apples and oranges could be compared")
  )

  init {
    oj1s.add(OpinionJustice(100, true, opinions[0], justices[0]))
    oj1s.add(OpinionJustice(101, false, opinions[0], justices[1]))
    oj1s.add(OpinionJustice(102, false, opinions[0], justices[2]))
    oj2s.add(OpinionJustice(200, true, opinions[1], justices[1]))
    oj3s.add(OpinionJustice(300, true, opinions[2], justices[3]))
  }

  @Test
  fun testGetAll() {
    whenever(opinionRepo.findAll()).thenReturn(opinions)
    val result = opinionService.getAll()
    assertThat(result).hasSize(3)
  }

  @Test
  fun testGetByCaseId() {
    whenever(opinionRepo.findByCaseId(100)).thenReturn(opinions)
    val result = opinionService.getByCaseId(100)
    assertThat(result).hasSize(3)
  }

  @Test
  fun testGetById() {
    whenever(opinionRepo.findById(2)).thenReturn(Optional.of(opinions[1]))
    val result = opinionService.getById(2)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.caseId).isEqualTo(100)
    assertThat(result?.opinionType).isEqualTo(OpinionType.CONCURRENCE)
    assertThat(result?.summary).isEqualTo("This does not mean other types of fruit cannot be compared")
    assertThat(result?.justices).hasSize(1)
    assertThat(result?.justices!![0].isAuthor).isTrue
    assertThat(result.justices[0].justiceId).isEqualTo(2)
    assertThat(result.justices[0].justiceName).isEqualTo("test justice 2")
  }

  @Test
  fun testGetById_null() {
    whenever(opinionRepo.findById(any())).thenReturn(Optional.empty())
    val result = opinionService.getById(100)
    assertThat(result).isNull()
  }

  @Test
  fun testDeleteOpinion() {
    whenever(opinionRepo.findById(2)).thenReturn(Optional.of(opinions[1]))
    opinionService.deleteOpinion(2)
    val args = argumentCaptor<Opinion>()
    verify(opinionRepo).delete(args.capture())
    assertThat(args.firstValue.id).isEqualTo(2)
  }

  @Test
  fun testDeleteOpinion_noId() {
    whenever(opinionRepo.findById(any())).thenReturn(Optional.empty())
    opinionService.deleteOpinion(100)
    verify(opinionRepo, never()).delete(any())
  }

  @Test
  fun testEditSummary() {
    whenever(opinionRepo.findById(3)).thenReturn(Optional.of(opinions[2]))
    whenever(opinionRepo.save<Opinion>(any())).thenAnswer { it.arguments[0] }
    val result = opinionService.editSummary(3, "this is a dissent")
    assertThat(result.id).isEqualTo(3)
    assertThat(result.opinionType).isEqualTo(OpinionType.DISSENT)
    assertThat(result.summary).isEqualTo("this is a dissent")

    verify(searchService).indexCase(100)
  }

  @Test
  fun testEditSummary_noOpinion() {
    whenever(opinionRepo.findById(any())).thenReturn(Optional.empty())
    assertThrows<OpinionNotFoundException> { opinionService.editSummary(100, "nooooo") }
  }

  @Test
  fun testCreateOpinion_noCase() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    val request = CreateOpinionRequest(500, OpinionType.MAJORITY, "There and back again", listOf())
    assertThrows<NoCaseIdException> { opinionService.createOpinion(request) }
  }

  @Test
  fun testCreateOpinion_noAuthor() {
    whenever(caseRepo.findById(100)).thenReturn(Optional.of(case))
    val request = CreateOpinionRequest(100, OpinionType.MAJORITY, "There and back again",
      listOf(CreateOpinionJusticeRequest(1, false), CreateOpinionJusticeRequest(2, false)))
    whenever(justiceRepo.findById(1)).thenReturn(Optional.of(justices[0]))
    whenever(justiceRepo.findById(2)).thenReturn(Optional.of(justices[1]))

    assertThrows<NoOpinionAuthorException> { opinionService.createOpinion(request) }
  }

  @Test
  fun testCreateOpinion_multipleAuthors() {
    whenever(caseRepo.findById(100)).thenReturn(Optional.of(case))
    val request = CreateOpinionRequest(100, OpinionType.MAJORITY, "There and back again",
      listOf(CreateOpinionJusticeRequest(1, true), CreateOpinionJusticeRequest(2, true)))
    whenever(justiceRepo.findById(1)).thenReturn(Optional.of(justices[0]))
    whenever(justiceRepo.findById(2)).thenReturn(Optional.of(justices[1]))

    assertThrows<MultipleOpinionAuthorException> { opinionService.createOpinion(request) }
  }

  @Test
  fun testCreateOpinion_invalidJustice() {
    whenever(caseRepo.findById(100)).thenReturn(Optional.of(case))
    val request = CreateOpinionRequest(100, OpinionType.MAJORITY, "There and back again",
      listOf(CreateOpinionJusticeRequest(1, true), CreateOpinionJusticeRequest(2, true)))
    whenever(justiceRepo.findById(1)).thenReturn(Optional.of(justices[0]))
    whenever(justiceRepo.findById(2)).thenReturn(Optional.empty())

    assertThrows<NoJusticeIdException> { opinionService.createOpinion(request) }
  }

  @Test
  fun testCreateOpinion() {
    whenever(caseRepo.findById(100)).thenReturn(Optional.of(case))
    val request = CreateOpinionRequest(100, OpinionType.MAJORITY, "There and back again",
      listOf(CreateOpinionJusticeRequest(1, true), CreateOpinionJusticeRequest(2, false)))
    whenever(justiceRepo.findById(1)).thenReturn(Optional.of(justices[0]))
    whenever(justiceRepo.findById(2)).thenReturn(Optional.of(justices[1]))
    whenever(opinionRepo.save<Opinion>(any())).thenAnswer {
      val opinion = it.arguments[0] as Opinion
      opinion.copy(id = 10)
    }

    val result = opinionService.createOpinion(request)
    assertThat(result.id).isEqualTo(10)
    assertThat(result.caseId).isEqualTo(100)
    assertThat(result.summary).isEqualTo("There and back again")
    assertThat(result.opinionType).isEqualTo(OpinionType.MAJORITY)
    assertThat(result.justices).hasSize(2)
    assertThat(result.justices[0].justiceId).isEqualTo(1)
    assertThat(result.justices[0].isAuthor).isTrue
    assertThat(result.justices[1].isAuthor).isFalse

    verify(searchService).indexCase(100)
  }
}