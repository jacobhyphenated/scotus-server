package com.hyphenated.scotus.case

import com.hyphenated.scotus.term.Term
import com.hyphenated.scotus.term.TermRepo
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.docket.*
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionJustice
import com.hyphenated.scotus.opinion.OpinionType
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
class CaseServiceTests {

  @Mock
  private lateinit var caseRepo: CaseRepo

  @Mock
  private lateinit var docketRepo: DocketRepo

  @Mock
  private lateinit var termRepo: TermRepo

  @InjectMocks
  private lateinit var caseService: CaseService

  private val terms = listOf(Term(1, "2019", "OT2019"), Term(2, "2020", "OT2020"))

  private val cases = listOf(
    Case(1, "Trump v. Vance", listOf(), "Can you criminally subpoena a sitting president?",
      "AFFIRMED", LocalDate.of(2019, 10, 1), "October", LocalDate.of(2020, 6, 30),
      "link.url", "7-2","No heightened standard for subpoenas exist for the president", terms[0], true,
      listOf(), listOf()
    ),
    Case(2, "McGirt v. Oklahoma", listOf(), "Is the area of eastern OK near Tulsa tribal land?", "REVERSED",
      LocalDate.of(2019, 11, 2), "November", LocalDate.of(2020, 6, 7), "url.com", "5-4",
      "The area is tribal land for the purposes of the major crimes act",  terms[0], true,
      listOf(), listOf()
    ),
    Case(3, "PennEast Pipeline Co. v. New Jersey", listOf(), "Can private gas companies use eminent domain on state lands?",
      null, null, null, null, null, null,null, terms[1], false,
      listOf(), listOf()
    )
  )

  @Test
  fun testGetAllCases() {
    whenever(caseRepo.findAll()).thenReturn(cases)
    val result = caseService.getAllCases()
    assertThat(result).hasSize(3)
  }

  @Test
  fun testGetTermCases() {
    whenever(caseRepo.findByTermId(1)).thenReturn(listOf(cases[0], cases[1]))
    val result = caseService.getTermCases(1)
    assertThat(result).hasSize(2)
  }

  @Test
  fun testGetCase() {
    val testCase = mockTestCases()[0]
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(testCase))

    val result = caseService.getCase(1)
    assertThat(result).isNotNull
    assertThat(result?.argumentDate).isEqualTo(LocalDate.of(2019, 10, 1))
    assertThat(result?.case).isEqualTo("Trump v. Vance")
    assertThat(result?.id).isEqualTo(1)
    assertThat(result?.result).isEqualTo("7-2")
    assertThat(result?.important).isTrue
    assertThat(result?.status).isEqualTo("AFFIRMED")
    assertThat(result?.term?.id).isEqualTo(1)
    assertThat(result?.opinions).hasSize(2)
    assertThat(result?.opinions!![0].opinionType).isEqualTo(OpinionType.MAJORITY)
    assertThat(result.opinions[0].justices).hasSize(2)
    assertThat(result.opinions[0].justices[0].justiceName).isEqualTo("Roberts")
    assertThat(result.opinions[0].justices[0].isAuthor).isTrue
    assertThat(result.opinions[0].justices[1].justiceName).isEqualTo("Sotomayor")
    assertThat(result.opinions[0].justices[1].isAuthor).isFalse
    assertThat(result.opinions[1].opinionType).isEqualTo(OpinionType.DISSENT)
    assertThat(result.opinions[1].justices[0].justiceName).isEqualTo("Alito")
    assertThat(result.opinions[1].justices[0].isAuthor).isTrue
    assertThat(result.dockets).hasSize(1)
    assertThat(result.dockets[0].docketNumber).isEqualTo("te-st")
    assertThat(result.alternateTitles).hasSize(1)
    assertThat(result.alternateTitles[0]).isEqualTo("Trump v. DA Vance")
  }

  @Test
  fun testGetCase_null() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    val result = caseService.getCase(10)
    assertThat(result).isNull()
  }

  @Test
  fun testCreateCase_noTerm() {
    whenever(docketRepo.findAllById(any())).thenReturn(listOf())
    whenever(termRepo.findById(any())).thenReturn(Optional.empty())
    val request = CreateCaseRequest("Carr v. Saul", "SSA Appointments reviewable by judge",
      10, false, listOf(50)
    )
    assertThrows<NoTermIdException> { caseService.createCase(request) }
  }

  @Test
  fun testCreateCase() {
    val docket = Docket(50, null, "Carr v. Saul", "19-001", Court(5, "te05", "test court 5"),
      "Not justiciable", null, "PENDING"
    )
    whenever(docketRepo.findAllById(listOf(50))).thenReturn(listOf(docket))
    whenever(termRepo.findById(2)).thenReturn(Optional.of(terms[1]))
    whenever(caseRepo.save<Case>(any())).thenAnswer {
      val c = it.arguments[0] as Case
      c.copy(id = 200)
    }
    val request = CreateCaseRequest("Carr v. Saul", "SSA Appointments reviewable by judge",
      2, false, listOf(50)
    )

    val result = caseService.createCase(request)
    assertThat(result.id).isEqualTo(200)
    assertThat(result.term.id).isEqualTo(2)
    assertThat(result.status).isEqualTo("GRANTED")
    assertThat(result.case).isEqualTo("Carr v. Saul")
    assertThat(result.opinions).hasSize(0)
    assertThat(result.dockets).hasSize(1)
    assertThat(result.dockets[0].docketNumber).isEqualTo("19-001")
    assertThat(result.result).isNull()
    assertThat(result.alternateTitles).hasSize(0)
  }

  @Test
  fun testCreateCase_alternateTitle() {
    val docket = Docket(56, null, "United States v. Texas", "19-001", Court(5, "te05", "test court 5"),
            "Not justiciable", null, "PENDING"
    )
    whenever(docketRepo.findAllById(listOf(56))).thenReturn(listOf(docket))
    whenever(termRepo.findById(2)).thenReturn(Optional.of(terms[1]))
    whenever(caseRepo.save<Case>(any())).thenAnswer {
      val c = it.arguments[0] as Case
      c.copy(id = 200)
    }
    val request = CreateCaseRequest("United States v. Texas", "Can the Biden administration end MPP without violating APA?",
            2, false, listOf(56), listOf("US v. Texas")
    )
    val result = caseService.createCase(request)
    assertThat(result.id).isEqualTo(200)
    assertThat(result.term.id).isEqualTo(2)
    assertThat(result.status).isEqualTo("GRANTED")
    assertThat(result.case).isEqualTo("United States v. Texas")
    assertThat(result.opinions).hasSize(0)
    assertThat(result.dockets).hasSize(1)
    assertThat(result.dockets[0].docketNumber).isEqualTo("19-001")
    assertThat(result.result).isNull()
    assertThat(result.alternateTitles).hasSize(1)
    assertThat(result.alternateTitles[0]).isEqualTo("US v. Texas")
  }

  @Test
  fun testEditCase_noId() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    val request = PatchCaseRequest(null, null, null, null, null, null, null, null, null, null, null, null)
    val result = caseService.editCase(10, request)
    assertThat(result).isNull()
  }

  @Test
  fun testEditCase_shortSummary() {
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(caseRepo.save<Case>(any())).thenAnswer { it.arguments[0] }
    val request = PatchCaseRequest("Trump v. Vaaance", "Criminal subpoena power",
      null, null, null, null, null, null, null, null,  null, null
    )
    val result = caseService.editCase(1, request)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(1)
    assertThat(result?.case).isEqualTo("Trump v. Vaaance")
    assertThat(result?.shortSummary).isEqualTo("Criminal subpoena power")
    assertThat(result?.status).isEqualTo("AFFIRMED")
    assertThat(result?.argumentDate).isEqualTo(LocalDate.of(2019, 10, 1))
    assertThat(result?.sitting).isEqualTo("October")
    assertThat(result?.decisionDate).isEqualTo(LocalDate.of(2020, 6, 30))
    assertThat(result?.decisionLink).isEqualTo("link.url")
    assertThat(result?.result).isEqualTo("7-2")
    assertThat(result?.decisionSummary).isEqualTo("No heightened standard for subpoenas exist for the president")
    assertThat(result?.term?.id).isEqualTo(1)
    assertThat(result?.important).isTrue
    assertThat(result?.alternateTitles).hasSize(0)
  }

  @Test
  fun testEditCase_term() {
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(termRepo.findById(2)).thenReturn(Optional.of(terms[1]))
    whenever(caseRepo.save<Case>(any())).thenAnswer { it.arguments[0] }
    val request = PatchCaseRequest(null, null, "REMANDED", LocalDate.of(2019, 11, 30),
      "November", null, "6-3", null, "updated.link",2, null, null
    )
    val result = caseService.editCase(1, request)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(1)
    assertThat(result?.case).isEqualTo("Trump v. Vance")
    assertThat(result?.shortSummary).isEqualTo("Can you criminally subpoena a sitting president?")
    assertThat(result?.status).isEqualTo("REMANDED")
    assertThat(result?.argumentDate).isEqualTo(LocalDate.of(2019, 11, 30))
    assertThat(result?.sitting).isEqualTo("November")
    assertThat(result?.decisionDate).isEqualTo(LocalDate.of(2020, 6, 30))
    assertThat(result?.decisionLink).isEqualTo("updated.link")
    assertThat(result?.result).isEqualTo("6-3")
    assertThat(result?.decisionSummary).isEqualTo("No heightened standard for subpoenas exist for the president")
    assertThat(result?.term?.id).isEqualTo(2)
    assertThat(result?.important).isTrue
    assertThat(result?.alternateTitles).hasSize(0)
  }

  @Test
  fun testEditCase_important() {
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(caseRepo.save<Case>(any())).thenAnswer { it.arguments[0] }
    val request = PatchCaseRequest(null, null, null, null, null,
      LocalDate.of(2020, 7, 10), null, "Not above the law", null,
      null, false, listOf("Trump v. DA Vance")
    )
    val result = caseService.editCase(1, request)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(1)
    assertThat(result?.case).isEqualTo("Trump v. Vance")
    assertThat(result?.shortSummary).isEqualTo("Can you criminally subpoena a sitting president?")
    assertThat(result?.status).isEqualTo("AFFIRMED")
    assertThat(result?.argumentDate).isEqualTo(LocalDate.of(2019, 10, 1))
    assertThat(result?.sitting).isEqualTo("October")
    assertThat(result?.decisionDate).isEqualTo(LocalDate.of(2020, 7, 10))
    assertThat(result?.result).isEqualTo("7-2")
    assertThat(result?.decisionSummary).isEqualTo("Not above the law")
    assertThat(result?.decisionLink).isEqualTo("link.url")
    assertThat(result?.term?.id).isEqualTo(1)
    assertThat(result?.important).isFalse
    assertThat(result?.alternateTitles).hasSize(1)
    assertThat(result?.alternateTitles!![0]).isEqualTo("Trump v. DA Vance")
  }

  @Test
  fun testRemoveArgumentDate() {
    whenever(caseRepo.findById(2)).thenReturn(Optional.of(cases[1]))
    whenever(caseRepo.save<Case>(any())).thenAnswer { it.arguments[0] }
    val result = caseService.removeArgumentDate(2)
    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.argumentDate).isNull()
    assertThat(result?.sitting).isNull()
  }

  @Test
  fun testRemoveArgumentDate_notFound() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    val result = caseService.removeArgumentDate(100)
    assertThat(result).isNull()
  }

  @Test
  fun testAssignDocket_noCase() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    assertThrows<CaseNotFoundException> { caseService.assignDocket(100, 50) }
  }

  @Test
  fun testAssignDocket_alreadyAssigned() {
    val dockets = listOf(Docket(50, cases[0], "Trump v. Vance", "te-st",
      Court(500, "te01", "test court 1"), "No special privileges", false, "DONE"
    ))
    val case = cases[0].copy(dockets = dockets)
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(case))
    val result = caseService.assignDocket(1, 50)
    assertThat(result.id).isEqualTo(1)
    assertThat(result.dockets[0].docketId).isEqualTo(50)

    verify(docketRepo, never()).findById(any())
    verify(caseRepo, never()).save<Case>(any())
  }

  @Test
  fun testAssignDocket_noDocketId() {
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(docketRepo.findById(50)).thenReturn(Optional.empty())
    assertThrows<NoDocketIdException> { caseService.assignDocket(1, 50) }
  }

  @Test
  fun testAssignDocket_assignedToAnotherCase() {
    val docket = Docket(50, cases[1], "Trump v. Vance", "te-st",
      Court(500, "te01", "test court 1"), "No special privileges", false, "DONE"
    )
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(docketRepo.findById(50)).thenReturn(Optional.of(docket))
    assertThrows<DocketAlreadyAssignedException> { caseService.assignDocket(1, 50) }
  }

  @Test
  fun testAssignDocket() {
    val docket = Docket(50, null, "Trump v. Vance", "te-st",
      Court(500, "te01", "test court 1"), "No special privileges", false, "DONE"
    )
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    whenever(docketRepo.findById(50)).thenReturn(Optional.of(docket))
    whenever(docketRepo.save<Docket>(any())).thenAnswer { it.arguments[0] }
    whenever(caseRepo.save<Case>(any())).thenAnswer { it.arguments[0] }

    val result = caseService.assignDocket(1, 50)
    assertThat(result.id).isEqualTo(1)
    assertThat(result.dockets[0].docketId).isEqualTo(50)

    val docketArgs = argumentCaptor<Docket>()
    val caseArgs = argumentCaptor<Case>()
    verify(docketRepo).save<Docket>(docketArgs.capture())
    verify(caseRepo).save<Case>(caseArgs.capture())
    assertThat(docketArgs.firstValue.case).isNotNull
    assertThat(docketArgs.firstValue.case?.id).isEqualTo(1)
    assertThat(caseArgs.firstValue.dockets[0].id).isEqualTo(50)
  }

  @Test
  fun testRemoveDocket_noCase() {
    whenever(caseRepo.findById(any())).thenReturn(Optional.empty())
    assertThrows<CaseNotFoundException> { caseService.removeDocket(100, 100) }
  }

  @Test
  fun testRemoveDocket_docketNotAssigned() {
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(cases[0]))
    caseService.removeDocket(1, 30)
    verify(docketRepo, never()).save<Docket>(any())
    verify(caseRepo, never()).save<Case>(any())
  }

  @Test
  fun testRemoveDocket() {
    val docket = Docket(50, cases[0], "Trump v. Vance", "te-st",
      Court(500, "te01", "test court 1"), "No special privileges", false, "DONE"
    )
    val case = cases[0].copy(dockets = listOf(docket))
    whenever(caseRepo.findById(1)).thenReturn(Optional.of(case))
    caseService.removeDocket(1, 50)

    val docketArgs = argumentCaptor<Docket>()
    val caseArgs = argumentCaptor<Case>()

    verify(docketRepo).save<Docket>(docketArgs.capture())
    verify(caseRepo).save<Case>(caseArgs.capture())

    assertThat(docketArgs.firstValue.id).isEqualTo(50)
    assertThat(docketArgs.firstValue.case).isNull()
    assertThat(caseArgs.firstValue.id).isEqualTo(1)
    assertThat(caseArgs.firstValue.dockets).hasSize(0)
  }

  @Test
  fun testTermSummary_noTermCases() {
    whenever(caseRepo.findByTermId(any())).thenReturn(listOf())
    assertThrows<NoTermIdException> { caseService.getTermSummary(5) }
  }

  @Test
  fun testTermSummary() {
    whenever(caseRepo.findByTermId(1)).thenReturn(mockTestCases())
    val result = caseService.getTermSummary(1)

    assertThat(result.termId).isEqualTo(1)
    assertThat(result.termEndDate).isEqualTo(LocalDate.of(2020, 6, 30))
    assertThat(result.courtSummary).hasSize(2)
    val courtSummary1 = result.courtSummary.first { it.court.id == 500L }
    assertThat(courtSummary1.cases).isEqualTo(2)
    assertThat(courtSummary1.affirmed).isEqualTo(1)
    assertThat(courtSummary1.reversedRemanded).isEqualTo(1)

    val courtSummary2 = result.courtSummary.first { it.court.id == 501L }
    assertThat(courtSummary2.cases).isEqualTo(1)
    assertThat(courtSummary2.affirmed).isEqualTo(1)
    assertThat(courtSummary2.reversedRemanded).isEqualTo(0)

    assertThat(result.justiceSummary).hasSize(3)
    val j1 = result.justiceSummary.first { it.justice.id == 100L }
    val j2 = result.justiceSummary.first { it.justice.id == 101L }
    val j3 = result.justiceSummary.first { it.justice.id == 102L }

    assertThat(j1.casesInMajority).isEqualTo(1)
    assertThat(j1.casesWithOpinion).isEqualTo(2)
    assertThat(j1.concurJudgementAuthor).isEqualTo(0)
    assertThat(j1.concurringAuthor).isEqualTo(0)
    assertThat(j1.dissentAuthor).isEqualTo(1)
    assertThat(j1.dissentJudgementAuthor).isEqualTo(0)
    assertThat(j1.majorityAuthor).isEqualTo(1)
    assertThat(j1.percentInMajority).isEqualTo(.5f)

    assertThat(j2.casesInMajority).isEqualTo(2)
    assertThat(j2.casesWithOpinion).isEqualTo(2)
    assertThat(j2.percentInMajority).isEqualTo(1f)
    assertThat(j2.majorityAuthor).isEqualTo(1)
    assertThat(j2.concurJudgementAuthor).isEqualTo(0)
    assertThat(j2.concurringAuthor).isEqualTo(0)
    assertThat(j2.dissentAuthor).isEqualTo(0)
    assertThat(j2.dissentJudgementAuthor).isEqualTo(0)

    assertThat(j3.casesInMajority).isEqualTo(1)
    assertThat(j3.casesWithOpinion).isEqualTo(2)
    assertThat(j3.percentInMajority).isEqualTo(.5f)
    assertThat(j3.majorityAuthor).isEqualTo(0)
    assertThat(j3.concurJudgementAuthor).isEqualTo(0)
    assertThat(j3.concurringAuthor).isEqualTo(1)
    assertThat(j3.dissentAuthor).isEqualTo(1)
    assertThat(j3.dissentJudgementAuthor).isEqualTo(0)

  }

  private fun mockTestCases(): List<Case> {
    val datePlaceholder = LocalDate.of(2000, 1, 1)
    val j1 = Justice(100, "Roberts", datePlaceholder, datePlaceholder, null)
    val j2 = Justice(101, "Sotomayor", datePlaceholder, datePlaceholder, null)
    val j3 = Justice(102, "Alito", datePlaceholder, datePlaceholder, null)

    val c1 = Court(500, "te01", "test court 1")
    val c2 = Court(501, "te02", "test court 2")

    val oj1 = mutableListOf<OpinionJustice>()
    val o1 = Opinion(100, cases[0], OpinionType.MAJORITY, oj1, "No special privileges for president")
    oj1.add(OpinionJustice(100, true, o1, j1))
    oj1.add(OpinionJustice(101, false, o1, j2))

    val oj2 = mutableListOf<OpinionJustice>()
    val o2 = Opinion(101, cases[0], OpinionType.DISSENT, oj2, "Yes special privileges")
    oj2.add(OpinionJustice(102, true, o2, j3))

    val dockets = listOf(Docket(100, cases[0], "Trump v. Vance", "te-st",
      c1, "No special privileges", false, "DONE"
    ))

    val alternateTitles = listOf(AlternateCaseTitle(100, cases[0], "Trump v. DA Vance"))

    val testCase1 = cases[0].copy(opinions = listOf(o1, o2), dockets = dockets, alternateTitles = alternateTitles)

    val oj3 = mutableListOf<OpinionJustice>()
    val o3 = Opinion(102, cases[1], OpinionType.MAJORITY, oj3, "This part of Oklahoma is part of the Creek nation")
    oj3.add(OpinionJustice(200, true, o3, j2))
    oj3.add(OpinionJustice(201, false, o3, j3))

    val oj4 = mutableListOf<OpinionJustice>()
    val o4 = Opinion(103, cases[1], OpinionType.CONCURRENCE, oj4, "Congress never nullified the treaty")
    oj4.add(OpinionJustice(202, true, o4, j3))

    val oj5 = mutableListOf<OpinionJustice>()
    val o5 = Opinion(104, cases[1], OpinionType.DISSENT, oj5, "The treaty was implicitly disestablished")
    oj5.add(OpinionJustice(203, true, o5, j1))

    val dockets2 = listOf(
      Docket(200, cases[1], "McGirt v. Oklahoma", "te-002", c1,
        "No valid treaty is in effect", true, "DONE"),
      Docket(201, cases[1], "Sharp v. Murphy", "te-002", c2,
        "Creek nation was established by treaty", false, "DONE")
    )

    val testCase2 = cases[1].copy(opinions = listOf(o3, o4, o5), dockets = dockets2)

    val dockets3 = listOf(Docket(300, cases[2], "PennEast Pipeline Co. v. New Jersey", "te-200",
      c1, "Go for the eminent domain", null, "PENDING"))
    val testCase3 = cases[2].copy(dockets = dockets3)
    return listOf(testCase1, testCase2, testCase3)
  }
}