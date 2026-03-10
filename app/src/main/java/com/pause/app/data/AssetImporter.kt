package com.pause.app.data

import android.content.Context
import com.pause.app.data.db.dao.BlacklistedDomainDao
import com.pause.app.data.db.dao.KeywordDao
import com.pause.app.data.db.entity.BlacklistedDomain
import com.pause.app.data.db.entity.KeywordEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports bundled keywords.json and domain_categories.json assets into the database on first setup.
 * Reads asset files and bulk-inserts into keyword_entries and blacklisted_domains tables.
 */
@Singleton
class AssetImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keywordDao: KeywordDao,
    private val blacklistedDomainDao: BlacklistedDomainDao
) {

    suspend fun importKeywordsIfNeeded() {
        if (keywordDao.getBundledCount() > 0) return

        val json = readAsset("keywords.json") ?: return
        val array = JSONArray(json)
        val keywords = mutableListOf<KeywordEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val keyword = obj.optString("keyword", "").lowercase().trim()
            val category = obj.optString("category", "CUSTOM")
            if (keyword.isNotBlank()) {
                keywords.add(
                    KeywordEntry(
                        keyword = keyword,
                        category = category,
                        isActive = true,
                        isBundled = true,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        if (keywords.isNotEmpty()) {
            keywordDao.insertAll(keywords)
        }
    }

    suspend fun importDomainCategoriesIfNeeded() {
        val existing = blacklistedDomainDao.getActiveDomainsAsList()
        if (existing.any { it.source == "CATEGORY" }) return

        val json = readAsset("domain_categories.json") ?: return
        val obj = JSONObject(json)
        val domains = mutableListOf<BlacklistedDomain>()
        val now = System.currentTimeMillis()

        for (key in obj.keys()) {
            val category = key
            val array = obj.optJSONArray(category) ?: continue
            for (i in 0 until array.length()) {
                val domain = array.optString(i, "").lowercase().trim()
                    .removePrefix("www.")
                if (domain.isNotBlank()) {
                    domains.add(
                        BlacklistedDomain(
                            domain = domain,
                            source = "CATEGORY",
                            isActive = true,
                            addedAt = now,
                            addedBy = "SYSTEM",
                            category = category,
                            pendingParentReview = false
                        )
                    )
                }
            }
        }
        domains.forEach { blacklistedDomainDao.insert(it) }
    }

    suspend fun importBundledAssetsIfNeeded() {
        importKeywordsIfNeeded()
        importDomainCategoriesIfNeeded()
    }

    private fun readAsset(fileName: String): String? {
        return try {
            context.assets.open(fileName).use { input ->
                BufferedReader(InputStreamReader(input)).use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
