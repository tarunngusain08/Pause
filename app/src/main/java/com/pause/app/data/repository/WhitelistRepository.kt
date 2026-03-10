package com.pause.app.data.repository

import com.pause.app.data.db.dao.WhitelistedDomainDao
import com.pause.app.data.db.entity.WhitelistedDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepository @Inject constructor(
    private val whitelistedDomainDao: WhitelistedDomainDao
) {

    fun getAll(): Flow<List<WhitelistedDomain>> =
        whitelistedDomainDao.getAll()

    suspend fun getAllAsList(): List<WhitelistedDomain> =
        whitelistedDomainDao.getAllAsList()

    suspend fun getByDomain(domain: String): WhitelistedDomain? =
        whitelistedDomainDao.getByDomain(normalizeDomain(domain))

    suspend fun addDomain(domain: WhitelistedDomain): Long =
        whitelistedDomainDao.insert(domain.copy(domain = normalizeDomain(domain.domain)))

    suspend fun removeByDomain(domain: String) =
        whitelistedDomainDao.deleteByDomain(normalizeDomain(domain))

    private fun normalizeDomain(domain: String): String =
        domain.lowercase()
            .removePrefix("www.")
            .trim()
            .takeIf { it.isNotBlank() } ?: domain
}
