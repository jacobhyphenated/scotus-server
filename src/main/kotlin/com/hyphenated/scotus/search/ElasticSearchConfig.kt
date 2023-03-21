package com.hyphenated.scotus.search

import org.opensearch.client.RestHighLevelClient
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration
import org.opensearch.data.client.orhlc.ClientConfiguration
import org.opensearch.data.client.orhlc.RestClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Profile("search")
@Configuration
@EnableElasticsearchRepositories("com.hyphenated.scotus.search")
class ElasticSearchConfig : AbstractOpenSearchConfiguration() {

  @Value("\${elasticsearch.user}")
  lateinit var elasticSearchUser: String

  @Value("\${elasticsearch.password}")
  lateinit var elasticSearchPassword: String

  @Value("\${elasticsearch.url}")
  lateinit var elasticSearchUrl: String

  override fun opensearchClient(): RestHighLevelClient {
    val clientConfiguration =  ClientConfiguration.builder()
      .connectedTo("$elasticSearchUrl:443")
      .usingSsl()
      .withBasicAuth(elasticSearchUser, elasticSearchPassword)
      .withSocketTimeout(10_000)
      .build()
    return RestClients.create(clientConfiguration).rest();
  }
}