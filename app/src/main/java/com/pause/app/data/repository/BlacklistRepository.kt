package com.pause.app.data.repository

import com.pause.app.data.db.dao.BlacklistedDomainDao
import com.pause.app.data.db.entity.BlacklistedDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlacklistRepository @Inject constructor(
    private val blacklistedDomainDao: BlacklistedDomainDao
) {

    fun getActiveDomains(): Flow<List<BlacklistedDomain>> =
        blacklistedDomainDao.getActiveDomains()

    suspend fun getActiveDomainsAsList(): List<BlacklistedDomain> =
        blacklistedDomainDao.getActiveDomainsAsList()

    suspend fun getByDomain(domain: String): BlacklistedDomain? =
        blacklistedDomainDao.getByDomain(normalizeDomain(domain))

    suspend fun addDomain(domain: BlacklistedDomain): Long =
        blacklistedDomainDao.insert(domain.copy(domain = normalizeDomain(domain.domain)))

    suspend fun updateDomain(domain: BlacklistedDomain) =
        blacklistedDomainDao.update(domain.copy(domain = normalizeDomain(domain.domain)))

    suspend fun removeById(id: Long) =
        blacklistedDomainDao.deleteById(id)

    suspend fun getByCategory(category: String): List<BlacklistedDomain> =
        blacklistedDomainDao.getByCategory(category)

    suspend fun getPendingReview(): List<BlacklistedDomain> =
        blacklistedDomainDao.getPendingReview()

    private fun normalizeDomain(domain: String): String =
        domain.lowercase()
            .removePrefix("www.")
            .trim()
            .takeIf { it.isNotBlank() } ?: domain
}
