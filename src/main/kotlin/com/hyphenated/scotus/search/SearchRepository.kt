package com.hyphenated.scotus.search

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface SearchRepository: ElasticsearchRepository<CaseSearchDocument, Long> {
}