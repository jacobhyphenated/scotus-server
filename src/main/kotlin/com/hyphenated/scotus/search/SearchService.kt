package com.hyphenated.scotus.search

import com.hyphenated.scotus.case.Case

interface SearchService {

  fun searchCases(searchTerm: String): List<Case>

  fun indexCase(caseId: Long)

  fun indexAllCases()
}