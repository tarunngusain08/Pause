package com.pause.app.service.webfilter.url

import com.pause.app.data.db.entity.UrlVisitLog
import com.pause.app.data.repository.UrlVisitLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class URLCaptureQueue @Inject constructor(
    private val urlVisitLogRepository: UrlVisitLogRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val pending = mutableListOf<UrlVisitLog>()
    private val recentDomains = mutableMapOf<String, Long>()

    companion object {
        private const val DEDUP_MS = 5000L
    }

    fun enqueue(
        url: String,
        domain: String,
        browserPackage: String,
        classification: URLClassification,
        wasBlocked: Boolean
    ) {
        scope.launch {
            val shouldFlush = mutex.withLock {
                val now = System.currentTimeMillis()
                val key = "$domain:$browserPackage"
                // Prune stale dedup entries to prevent unbounded growth
                val staleKeys = recentDomains.entries
                    .filter { now - it.value >= DEDUP_MS }
                    .map { it.key }
                staleKeys.forEach { recentDomains.remove(it) }

                if (recentDomains[key] != null && now - (recentDomains[key] ?: 0) < DEDUP_MS) {
                    return@withLock false
                }
                recentDomains[key] = now
                pending.add(
                    UrlVisitLog(
                        fullUrl = url.take(500),
                        domain = domain,
                        browserPackage = browserPackage,
                        visitedAt = now,
                        wasBlocked = wasBlocked,
                        parentReviewed = false,
                        classification = classification.name
                    )
                )
                pending.size >= 10
            }
            // Flush outside the lock to avoid re-entrant deadlock
            if (shouldFlush) flushInternal()
        }
    }

    suspend fun flush() {
        flushInternal()
    }

    private suspend fun flushInternal() {
        val toWrite = mutex.withLock {
            if (pending.isEmpty()) return
            val snapshot = pending.toList()
            pending.clear()
            snapshot
        }
        toWrite.forEach { urlVisitLogRepository.insert(it) }
    }
}
