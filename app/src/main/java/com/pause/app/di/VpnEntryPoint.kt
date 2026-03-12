package com.pause.app.di

import com.pause.app.data.repository.BlacklistRepository
import com.pause.app.service.webfilter.BlocklistMatcher
import com.pause.app.data.repository.WebFilterConfigRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VpnEntryPoint {
    fun getBlacklistRepository(): BlacklistRepository
    fun getWebFilterConfigRepository(): WebFilterConfigRepository
    fun getBlocklistMatcher(): BlocklistMatcher
}
