package com.hyphenated.scotus.search

import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.RestClients
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Profile("search")
@Configuration
@EnableElasticsearchRepositories("com.hyphenated.scotus.search")
class ElasticSearchConfig : AbstractElasticsearchConfiguration() {

  @Value("\${elasticsearch.user}")
  lateinit var elasticSearchUser: String

  @Value("\${elasticsearch.password}")
  lateinit var elasticSearchPassword: String

  @Value("\${elasticsearch.url}")
  lateinit var elasticSearchUrl: String

  @Bean
  override fun elasticsearchClient(): RestHighLevelClient {
    val clientConfig = ClientConfiguration.builder()
        .connectedTo("$elasticSearchUrl:443")
        .usingSsl()
        .withBasicAuth(elasticSearchUser, elasticSearchPassword)
        .withSocketTimeout(10_000)
        .build()
    return RestClients.create(clientConfig).rest()
  }
}