package com.hyphenated.scotus.case

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.format.DateTimeFormatter

@Service
class CaseURLValidationClient(private val restTemplate: RestTemplate) {

  // consider redis cache
  // in memory cache is acceptable for now
  private val cache: MutableMap<Long, String> = mutableMapOf()

  fun getValidCaseLink(case: Case): String? {
    if (case.decisionDate == null) {
      return case.decisionLink
    }
    checkCache(case)?.let { return it }
    return case.decisionLink?.let { decisionLink ->
      log.debug("checking for archive link for $decisionLink")
      val archiveDate = case.decisionDate
        .plusDays(7)
        .format(DateTimeFormatter.ofPattern("yyyyMMdd"))

      try {
        val requestUri = "https://archive.org/wayback/available?url=$decisionLink&timestamp=$archiveDate"
        val result = restTemplate.getForObject(requestUri, ArchiveResult::class.java)
        log.debug("Archive URL: ${result?.archivedSnapshot?.closest?.url}")
        result?.archivedSnapshot?.closest?.url?.let {
          // if the url is valid, then modify the url to include "if_" to use as an iframe only URL
          val archivePart = it.indexOf("/http")
          if (archivePart == -1) {
            it
          } else {
            val link = "${it.substring(0, archivePart)}if_${it.substring(archivePart)}"
            log.debug("final archive link: $link")
            if (case.id != null) {
              cache[case.id] = link
            }
            link
          }
        } ?: decisionLink
      }catch(e: Exception) {
        log.error("Error fetching URL from archive.org", e)
        decisionLink
      }

    }
  }

  fun checkCache(case: Case): String? {
    return cache[case.id].also {
      log.debug("Checking decision archive link cache: $it")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(CaseURLValidationClient::class.java)
  }
}

private class ArchiveResult (
  @JsonProperty("archived_snapshots")
  val archivedSnapshot: Archive?
)

private class ArchiveSnapshot (
  val url: String
)

private class Archive (
  val closest: ArchiveSnapshot?
)