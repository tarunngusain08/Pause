package com.pause.app.service.webfilter.url

import com.pause.app.data.db.entity.KeywordEntry
import com.pause.app.data.repository.KeywordRepository
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

data class KeywordMatch(
    val keyword: String,
    val category: String,
    val segment: Segment
) {
    enum class Segment { DOMAIN, PATH, QUERY }
}

@Singleton
class KeywordMatcher @Inject constructor(
    private val keywordRepository: KeywordRepository
) {
    private var keywords: List<KeywordEntry> = emptyList()
    private var lastReload = 0L

    suspend fun checkAll(domain: String, path: String, query: String): KeywordMatch? {
        reloadIfNeeded()
        val domainNorm = normalize(domain)
        val pathNorm = normalize(path)
        val queryNorm = normalize(query)
        for (kw in keywords.filter { it.isActive }) {
            val term = normalize(kw.keyword)
            if (term.isEmpty()) continue
            when {
                domainNorm.contains(term) -> return KeywordMatch(kw.keyword, kw.category, KeywordMatch.Segment.DOMAIN)
                pathNorm.contains(term) -> return KeywordMatch(kw.keyword, kw.category, KeywordMatch.Segment.PATH)
                queryNorm.contains(term) -> return KeywordMatch(kw.keyword, kw.category, KeywordMatch.Segment.QUERY)
            }
        }
        return null
    }

    suspend fun reloadFromDB() {
        keywords = keywordRepository.getActiveKeywords()
        lastReload = System.currentTimeMillis()
    }

    private suspend fun reloadIfNeeded() {
        if (keywords.isEmpty() || System.currentTimeMillis() - lastReload > 60_000) {
            reloadFromDB()
        }
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
}
