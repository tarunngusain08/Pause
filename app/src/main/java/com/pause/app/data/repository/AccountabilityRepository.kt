package com.pause.app.data.repository

import com.pause.app.data.db.dao.AccountabilityDao
import com.pause.app.data.db.entity.Accountability
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountabilityRepository @Inject constructor(
    private val accountabilityDao: AccountabilityDao
) {

    suspend fun getPartner(): Accountability? = accountabilityDao.getPartner()

    fun getPartnerFlow(): Flow<Accountability?> = accountabilityDao.getPartnerFlow()

    suspend fun savePartner(accountability: Accountability): Long =
        accountabilityDao.insert(accountability)

    suspend fun updatePartner(accountability: Accountability) =
        accountabilityDao.update(accountability)

    suspend fun removePartner() = accountabilityDao.deleteAll()
}
