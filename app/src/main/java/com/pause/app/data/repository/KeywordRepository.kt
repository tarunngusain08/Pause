package com.pause.app.data.repository

import com.pause.app.data.db.dao.KeywordDao
import com.pause.app.data.db.entity.KeywordEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordRepository @Inject constructor(
    private val keywordDao: KeywordDao
) {

    suspend fun getActiveKeywords(): List<KeywordEntry> =
        keywordDao.getActiveKeywords()

    fun getActiveKeywordsFlow(): Flow<List<KeywordEntry>> =
        keywordDao.getActiveKeywordsFlow()

    suspend fun addKeyword(keyword: KeywordEntry): Long =
        keywordDao.insert(keyword.copy(keyword = normalizeKeyword(keyword.keyword)))

    suspend fun addKeywords(keywords: List<KeywordEntry>) {
        val normalized = keywords.map { it.copy(keyword = normalizeKeyword(it.keyword)) }
        keywordDao.insertAll(normalized)
    }

    suspend fun removeById(id: Long) =
        keywordDao.deleteById(id)

    suspend fun toggleCategory(category: String, enabled: Boolean) =
        keywordDao.toggleCategory(category, enabled)

    suspend fun getBundledCount(): Int =
        keywordDao.getBundledCount()

    private fun normalizeKeyword(keyword: String): String =
        keyword.lowercase().trim().takeIf { it.isNotBlank() } ?: keyword
}
