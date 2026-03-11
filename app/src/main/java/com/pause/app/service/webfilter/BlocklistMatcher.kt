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
    @Volatile private var snapshot: Set<String>? = null
    @Volatile private var lastReload = 0L

    suspend fun isBlocked(domain: String): Boolean {
        val current = getSnapshot()
        val normalized = normalize(domain)
        if (current.contains(normalized)) return true
        return current.any {
            if (!it.startsWith("*.")) return@any false
            val suffix = it.removePrefix("*.").lowercase()
            normalized == suffix || normalized.endsWith(".$suffix")
        }
    }

    suspend fun reloadFromDB() {
        val loaded = blacklistRepository.getActiveDomainsAsList().map { it.domain }.toHashSet()
        mutex.withLock {
            snapshot = loaded
            lastReload = System.currentTimeMillis()
        }
    }

    private suspend fun getSnapshot(): Set<String> {
        // Fast path: check without lock
        snapshot?.takeIf { System.currentTimeMillis() - lastReload < 60_000 }?.let { return it }
        // Slow path: acquire lock, check again, then reload
        return mutex.withLock {
            snapshot?.takeIf { System.currentTimeMillis() - lastReload < 60_000 } ?: run {
                val fresh = blacklistRepository.getActiveDomainsAsList().map { it.domain }.toHashSet()
                snapshot = fresh
                lastReload = System.currentTimeMillis()
                fresh
            }
        }
    }

    private fun normalize(domain: String): String =
        domain.lowercase().removePrefix("www.").trim()
}
