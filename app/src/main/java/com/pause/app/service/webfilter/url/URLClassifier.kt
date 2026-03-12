package com.pause.app.service.webfilter.url

import com.pause.app.data.repository.BlacklistRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class URLClassification {
    CLEAN,
    KEYWORD_MATCH,
    BLACKLISTED,
    WHITELISTED
}

@Singleton
class URLClassifier @Inject constructor(
    private val blacklistRepository: BlacklistRepository,
    private val keywordMatcher: KeywordMatcher
) {

    suspend fun classify(url: String, domain: String): Pair<URLClassification, KeywordMatch?> {
        val path = extractPath(url)
        val query = extractQuery(url)
        if (blacklistRepository.isDomainBlacklisted(domain)) {
            return URLClassification.BLACKLISTED to null
        }
        val keywordMatch = keywordMatcher.checkAll(domain, path, query)
        if (keywordMatch != null) {
            return URLClassification.KEYWORD_MATCH to keywordMatch
        }
        return URLClassification.CLEAN to null
    }

    suspend fun containsKeyword(url: String, domain: String): KeywordMatch? {
        val path = extractPath(url)
        val query = extractQuery(url)
        return keywordMatcher.checkAll(domain, path, query)
    }

    private fun extractPath(url: String): String {
        return try {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd < 0) return ""
            val pathStart = url.indexOf('/', schemeEnd + 3)
            if (pathStart < 0) "" else url.substring(pathStart)
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractQuery(url: String): String {
        val q = url.indexOf('?')
        return if (q < 0) "" else url.substring(q + 1)
    }
}
