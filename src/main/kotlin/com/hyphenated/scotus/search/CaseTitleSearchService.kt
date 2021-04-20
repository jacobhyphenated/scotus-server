package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.Case
import com.hyphenated.scotus.case.CaseRepo
import com.hyphenated.scotus.docket.DocketRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class CaseTitleSearchService(private val caseRepo: CaseRepo,
                             private val docketRepo: DocketRepo) {

  @Transactional
  fun searchCases(searchTerm: String): List<Case> = runBlocking {
    val caseSearchResults =  async {
      caseRepo.findByCaseIgnoreCaseContaining(searchTerm)
    }
    val docketSearchResults = async {
      docketRepo.findByTitleIgnoreCaseContaining(searchTerm).mapNotNull { it.case }
    }
    val alternateTitleSearchResults = async {
      caseRepo.findByAlternateTitles_titleIgnoreCaseContaining(searchTerm)
    }
    val results = caseSearchResults.await().toMutableList()
    results.addAll(alternateTitleSearchResults.await())
    results.addAll(docketSearchResults.await())
    results.map { it.id }
      .toSet()
      .map { results.first { c -> c.id == it } }
  }
}