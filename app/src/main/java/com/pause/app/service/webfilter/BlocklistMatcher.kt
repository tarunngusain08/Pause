package com.pause.app.service.webfilter

import com.pause.app.data.repository.BlacklistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistMatcher @Inject constructor(
    private val blacklistRepository: BlacklistRepository
) {
    private val domains = mutableSetOf<String>()
    private var lastReload = 0L

    suspend fun isBlocked(domain: String): Boolean {
        reloadIfNeeded()
        val normalized = normalize(domain)
        if (domains.contains(normalized)) return true
        return domains.any { it.startsWith("*.") && domain.endsWith(it.removePrefix("*.")) }
    }

    suspend fun reloadFromDB() {
        domains.clear()
        blacklistRepository.getActiveDomainsAsList().forEach { domains.add(it.domain) }
        lastReload = System.currentTimeMillis()
    }

    private suspend fun reloadIfNeeded() {
        if (domains.isEmpty() || System.currentTimeMillis() - lastReload > 60_000) {
            reloadFromDB()
        }
    }

    private fun normalize(domain: String): String =
        domain.lowercase().removePrefix("www.").trim()
}
