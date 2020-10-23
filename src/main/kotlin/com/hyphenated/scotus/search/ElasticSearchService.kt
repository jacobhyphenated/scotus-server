package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.case.CaseService
import com.hyphenated.scotus.docket.CaseNotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("search")
class ElasticSearchService(private val searchRepository: SearchRepository,
                           private val caseRepo: CaseRepo,
                           private val client: ElasticsearchRestTemplate): SearchService {
  override fun searchCases(searchTerm: String): List<Case> {
    log.debug("Elastic Search: $searchTerm")
    val query = NativeSearchQueryBuilder().withQuery(boolQuery()
        .should(multiMatchQuery(searchTerm)
            .field("title")
            .field("docketTitles")
            .fuzziness(Fuzziness.TWO)
            .operator(Operator.AND))
        .should(multiMatchQuery(searchTerm)
            .field("shortSummary")
            .field("decision")
            .fuzziness(Fuzziness.ONE)
            .operator(Operator.AND))
        .should(matchPhraseQuery("opinions", searchTerm)
            .slop(1))
        .should(matchPhraseQuery("docketSummaries", searchTerm)
            .slop(1)))
        .build()
    val searchResult = client.search(query, CaseSearchDocument::class.java, IndexCoordinates.of("scotus_case"))
    if (searchResult.isEmpty) {
      return emptyList()
    }
    val ids = searchResult.sortedByDescending { it.score }
        .subList(0, 10.coerceAtMost(searchResult.totalHits.toInt()))
        .mapNotNull {
          log.debug("${it.id} - ${it.score}")
          it.id?.toLong()
        }
    val cases = caseRepo.findByIdIn(ids)
    return ids.mapNotNull { cases.find { c -> c.id == it } }
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  override fun indexCase(caseId: Long) {
    val case = caseRepo.findByIdOrNull(caseId) ?: throw CaseNotFoundException(caseId)
    searchRepository.save(case.toDocument())
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  override fun indexAllCases() {
    caseRepo.findAll().forEachParallel { indexCase(it.id!!) }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ElasticSearchService::class.java)
  }
}

fun Case.toDocument(): CaseSearchDocument {
  return CaseSearchDocument(
      id = this.id!!,
      title = this.case,
      shortSummary = this.shortSummary,
      decision = this.decisionSummary,
      docketTitles = this.dockets.map { it.title },
      docketSummaries = this.dockets.map { it.lowerCourtRuling },
      opinions = this.opinions.map { it.summary }
  )
}

fun <A>Collection<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
  map { async { f(it) } }.forEach { it.await() }
}