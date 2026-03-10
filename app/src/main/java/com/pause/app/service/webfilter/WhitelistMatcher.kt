package com.pause.app.service.webfilter

import com.pause.app.data.repository.WhitelistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistMatcher @Inject constructor(
    private val whitelistRepository: WhitelistRepository
) {
    private val domains = mutableSetOf<String>()
    private var lastReload = 0L

    suspend fun isWhitelisted(domain: String): Boolean {
        reloadIfNeeded()
        return domains.contains(normalize(domain))
    }

    suspend fun reloadFromDB() {
        domains.clear()
        whitelistRepository.getAllAsList().forEach { domains.add(it.domain) }
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
