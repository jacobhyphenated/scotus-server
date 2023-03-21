package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.docket.CaseNotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.opensearch.common.unit.Fuzziness
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilders.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Exception

@Service
@Profile("search")
class ElasticSearchService(private val searchRepository: SearchRepository,
                           private val caseTitleSearchService: CaseTitleSearchService,
                           private val caseRepo: CaseRepo,
                           private val client: ElasticsearchOperations
): SearchService {

  override fun searchCases(searchTerm: String): List<Case> {
    log.debug("Elastic Search: $searchTerm")
    return try {
      elasticSearchLookup(searchTerm)
    } catch (e: Exception) {
      log.error("elasticsearch lookup error", e)
      caseTitleSearchService.searchCases(searchTerm)
    }
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  override fun indexCase(caseId: Long) {
    val case = caseRepo.findByIdOrNull(caseId) ?: throw CaseNotFoundException(caseId)
    updateCaseIndex(case)
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  override fun indexAllCases() = runBlocking {
    caseRepo.findAll().forEachParallel { updateCaseIndex(it) }
  }

  private fun updateCaseIndex(case: Case) {
    try {
      searchRepository.save(case.toDocument())
    } catch (e: Exception) {
      log.error("elasticsearch index error", e)
    }
  }

  private fun elasticSearchLookup(searchTerm: String): List<Case> {
    val query = NativeSearchQueryBuilder().withQuery(boolQuery()
      .should(multiMatchQuery(searchTerm)
        .field("title")
        .field("docketTitles")
        .field("alternateTitles")
        .fuzziness(Fuzziness.TWO)
        .operator(Operator.AND))
      .should(termQuery("docketNumbers", searchTerm)
        .boost(2.0f))
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
    // Remove 0.0 or other very small scores that sometimes get returned
    val scoreResults = searchResult.filter { it.score > 0.1 }

    val ids = scoreResults
      .sortedByDescending { it.score }
      .subList(0, 10.coerceAtMost(scoreResults.count()))
      .mapNotNull {
        log.debug("${it.id} - ${it.score}")
        it.id?.toLong()
      }
    val cases = caseRepo.findByIdIn(ids)
    return ids.mapNotNull { cases.find { c -> c.id == it } }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ElasticSearchService::class.java)
  }
}

fun Case.toDocument(): CaseSearchDocument {
  return CaseSearchDocument(
      id = this.id!!,
      title = this.case,
      alternateTitles = this.alternateTitles.map { it.title },
      shortSummary = this.shortSummary,
      decision = this.decisionSummary,
      docketTitles = this.dockets.map { it.title },
      docketSummaries = this.dockets.map { it.lowerCourtRuling },
      opinions = this.opinions.map { it.summary },
      docketNumbers = this.dockets.map { it.docketNumber }
  )
}

suspend fun <T>Collection<T>.forEachParallel(f: suspend (T) -> Unit): Unit = coroutineScope {
  map { async { f(it) } }.forEach { it.await() }
}