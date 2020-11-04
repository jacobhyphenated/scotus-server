package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.docket.DocketRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Profile("!search")
class DBSearchService(private val caseRepo: CaseRepo,
                      private val docketRepo: DocketRepo): SearchService {

  @Transactional
  override fun searchCases(searchTerm: String): List<Case> = runBlocking {
    val caseSearchResults =  async {
      caseRepo.findByCaseIgnoreCaseContaining(searchTerm)
    }
    val docketSearchResults = async {
      docketRepo.findByTitleIgnoreCaseContaining(searchTerm).mapNotNull { it.case }
    }
    val results = caseSearchResults.await().toMutableList()
    results.addAll(docketSearchResults.await())
    results.map { it.id }
        .toSet()
        .map { results.first { c -> c.id == it } }
  }

  @PreAuthorize("hasRole('ADMIN')")
  override fun indexCase(caseId: Long) {
    // noop
    log.debug("Index request for case $caseId (noop on DB Search Service)")
  }

  @PreAuthorize("hasRole('ADMIN')")
  override fun indexAllCases() {
    // noop
    log.debug("Index request for all cases (noop on DB Search Service)")
  }

  companion object {
    private val log = LoggerFactory.getLogger(DBSearchService::class.java)
  }
}