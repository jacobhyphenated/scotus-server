package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.AlternateCaseTitle
import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.case.term.Term
import com.hyphenated.scotus.court.Court
import com.hyphenated.scotus.docket.Docket
import com.hyphenated.scotus.justice.Justice
import com.hyphenated.scotus.opinion.Opinion
import com.hyphenated.scotus.opinion.OpinionJustice
import com.hyphenated.scotus.opinion.OpinionType
import com.hyphenated.scotus.user.UserEntity
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ElasticSearchServiceTests {

  @Mock
  private lateinit var searchRepository: SearchRepository

  @Mock
  private lateinit var caseTitleSearchService: CaseTitleSearchService

  @Mock
  private lateinit var caseRepo: CaseRepo

  @Mock
  private lateinit var client: ElasticsearchRestTemplate

  @InjectMocks
  private lateinit var service: ElasticSearchService

  @Test
  fun testSearchCases_dbFallback() {
    whenever(client.search<CaseSearchDocument>(any<NativeSearchQuery>(), any(), any())).thenThrow(RuntimeException())
    whenever(caseTitleSearchService.searchCases("teststring")).thenReturn(listOf(
      Case(3, "a v. b", emptyList(), "", "ARGUED", null, null, null, null, null, null,
        Term(1, "", ""), false, emptyList(), emptyList()),
      Case(14, "1 v. 2", emptyList(), "", "ARGUED", null, null, null, null, null, null,
        Term(1, "", ""), false, emptyList(), emptyList())
    ))

    val result = service.searchCases("teststring")
    assertThat(result).hasSize(2)
  }

  @Test
  fun testCaseToDocument() {
    val c = Case(3, "test case v. junit", emptyList(), "testing the elasticsearch document map",
      "GRANTED", LocalDate.of(2000, 1, 15), "January",
      LocalDate.of(2000, 6, 30), null, "9-0", "Case is properly mapped",
      Term(5, "2019-2020", "OT2019"), true, emptyList(), emptyList()
    )
    val alternateTitles = listOf(AlternateCaseTitle(2,c, "mapping case v. testers"))
    val o1 = Opinion(10, c, OpinionType.MAJORITY, emptyList(), "Case mapping wins the day")
    val o2 = Opinion(11, c, OpinionType.DISSENT, emptyList(), "This mapping will never work")
    val j1 = Justice(1, "J1", LocalDate.of(1950, 1, 1), LocalDate.of(1990, 1, 1), null)
    val j2 = Justice(2, "J2", LocalDate.of(1950, 1, 1), LocalDate.of(1990, 1, 1), null)
    val j3 = Justice(3, "J3", LocalDate.of(1950, 1, 1), LocalDate.of(1990, 1, 1), null)
    val opinions = listOf(
      o1.copy(opinionJustices = listOf(OpinionJustice(50, true, o1, j1), OpinionJustice(51, false, o1, j2))),
      o2.copy(opinionJustices = listOf(OpinionJustice(51, true, o2, j3)))
    )
    val d1 = Docket(1, c, "test case v. junit", "000-001", Court(1, "C1", "Court 1"),
      "We truthfully expect the mapping to work", false, "DONE")
    val testCase = c.copy(alternateTitles = alternateTitles, opinions = opinions, dockets = listOf(d1))

    val mapped = testCase.toDocument()
    assertThat(mapped.id).isEqualTo(3)
    assertThat(mapped.title).isEqualTo("test case v. junit")
    assertThat(mapped.alternateTitles).containsExactly("mapping case v. testers")
    assertThat(mapped.shortSummary).isEqualTo("testing the elasticsearch document map")
    assertThat(mapped.decision).isEqualTo("Case is properly mapped")
    assertThat(mapped.docketTitles).containsExactly("test case v. junit")
    assertThat(mapped.docketSummaries).containsExactly("We truthfully expect the mapping to work")
    assertThat(mapped.docketNumbers).containsExactly("000-001")
    assertThat(mapped.opinions).containsExactly("Case mapping wins the day", "This mapping will never work")
  }
}