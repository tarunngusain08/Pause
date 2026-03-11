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
        // insertCategoryDomainsIfNone performs an atomic check-then-insert within a transaction
        if (domains.isNotEmpty()) {
            blacklistedDomainDao.insertCategoryDomainsIfNone(domains)
        }
    }

    /**
     * Seeds known DNS-over-HTTPS resolver domains as non-deletable SYSTEM entries.
     * These domains are used to bypass VPN-based filtering, so we block them at the DNS level.
     */
    suspend fun seedDoHBlocklistIfNeeded() {
        val now = System.currentTimeMillis()
        for (domain in DOH_RESOLVER_DOMAINS) {
            val existing = blacklistedDomainDao.getByDomain(domain)
            if (existing == null) {
                blacklistedDomainDao.insert(
                    BlacklistedDomain(
                        domain = domain,
                        source = "SYSTEM",
                        isActive = true,
                        addedAt = now,
                        addedBy = "SYSTEM",
                        category = "DOH_BYPASS",
                        pendingParentReview = false
                    )
                )
            }
        }
    }

    suspend fun importBundledAssetsIfNeeded() {
        importKeywordsIfNeeded()
        importDomainCategoriesIfNeeded()
        seedDoHBlocklistIfNeeded()
    }

    companion object {
        /** Known DNS-over-HTTPS resolver domains that could bypass VPN-based DNS filtering. */
        private val DOH_RESOLVER_DOMAINS = listOf(
            "dns.google",
            "dns64.dns.google",
            "cloudflare-dns.com",
            "1dot1dot1dot1.cloudflare-dns.com",
            "dns.quad9.net",
            "dns9.quad9.net",
            "doh.opendns.com",
            "doh.familyshield.opendns.com",
            "doh.cleanbrowsing.org",
            "adblock.dns.mullvad.net",
            "base.dns.mullvad.net",
            "doh.dns.sb",
            "resolver2.dns.watch"
        )
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
