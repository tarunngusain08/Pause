package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_filter_config")
data class WebFilterConfig(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "vpn_enabled") val vpnEnabled: Boolean = false,
    @ColumnInfo(name = "url_reader_enabled") val urlReaderEnabled: Boolean = false,
    @ColumnInfo(name = "keyword_filter_enabled") val keywordFilterEnabled: Boolean = false,
    @ColumnInfo(name = "auto_blacklist_on_keyword_match") val autoBlacklistOnKeywordMatch: Boolean = false,
    @ColumnInfo(name = "notify_parent_on_auto_block") val notifyParentOnAutoBlock: Boolean = false,
    @ColumnInfo(name = "safe_search_enforcement") val safeSearchEnforcement: Boolean = false,
    @ColumnInfo(name = "youtube_restricted_mode") val youtubeRestrictedMode: Boolean = false,
    @ColumnInfo(name = "block_incognito") val blockIncognito: Boolean = false,
    @ColumnInfo(name = "daily_browsing_budget_minutes") val dailyBrowsingBudgetMinutes: Int = 0,
    @ColumnInfo(name = "upstream_dns") val upstreamDns: String = "8.8.8.8"
)
