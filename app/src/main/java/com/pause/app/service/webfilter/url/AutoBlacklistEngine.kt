package com.pause.app.service.webfilter.url

import com.pause.app.data.db.dao.PendingReviewDao
import com.pause.app.data.db.entity.BlacklistedDomain
import com.pause.app.data.db.entity.PendingReview
import com.pause.app.data.repository.BlacklistRepository
import com.pause.app.data.repository.WebFilterConfigRepository
import com.pause.app.service.webfilter.BlocklistMatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoBlacklistEngine @Inject constructor(
    private val blacklistRepository: BlacklistRepository,
    private val webFilterConfigRepository: WebFilterConfigRepository,
    private val blocklistMatcher: BlocklistMatcher,
    private val pendingReviewDao: PendingReviewDao
) {

    suspend fun onKeywordMatch(domain: String, keyword: String, url: String) {
        val config = webFilterConfigRepository.getConfig() ?: return
        if (!config.autoBlacklistOnKeywordMatch) return

        val normalized = domain.lowercase().removePrefix("www.").trim()
        if (blacklistRepository.getByDomain(normalized) != null) return

        blacklistRepository.addDomain(
            BlacklistedDomain(
                domain = normalized,
                source = "AUTO_KEYWORD",
                isActive = true,
                addedAt = System.currentTimeMillis(),
                addedBy = "AUTO",
                category = null,
                pendingParentReview = true
            )
        )
        blocklistMatcher.reloadFromDB()

        pendingReviewDao.insert(
            PendingReview(
                domain = normalized,
                triggerKeyword = keyword,
                flaggedAt = System.currentTimeMillis(),
                status = "PENDING"
            )
        )
    }
}
