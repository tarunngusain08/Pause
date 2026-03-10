package com.pause.app.service.webfilter

import com.pause.app.data.repository.BlacklistRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistMatcher @Inject constructor(
    private val blacklistRepository: BlacklistRepository
) {
    private val mutex = Mutex()
    /** null = never loaded; empty set = loaded but database is empty */
    private var snapshot: Set<String>? = null
    private var lastReload = 0L

    suspend fun isBlocked(domain: String): Boolean {
        reloadIfNeeded()
        val normalized = normalize(domain)
        val current = mutex.withLock { snapshot } ?: return false
        if (current.contains(normalized)) return true
        return current.any {
            if (!it.startsWith("*.")) return@any false
            val suffix = it.removePrefix("*.").lowercase()
            normalized == suffix || normalized.endsWith(".$suffix")
        }
    }

    suspend fun reloadFromDB() {
        val loaded = blacklistRepository.getActiveDomainsAsList().map { it.domain }.toSet()
        mutex.withLock {
            snapshot = loaded
            lastReload = System.currentTimeMillis()
        }
    }

    private suspend fun reloadIfNeeded() {
        val needsReload = mutex.withLock {
            snapshot == null || System.currentTimeMillis() - lastReload > 60_000
        }
        if (needsReload) reloadFromDB()
    }

    private fun normalize(domain: String): String =
        domain.lowercase().removePrefix("www.").trim()
}
