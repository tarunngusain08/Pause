package com.pause.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val APP_SELECTION = "app_selection"
    const val STRICT_SETUP = "strict_setup"
    const val PARENTAL_SETUP = "parental_setup"
    const val PARENT_DASHBOARD = "parent_dashboard"
    const val CHILD_STATUS = "child_status"
    const val SETTINGS = "settings"
    const val FOCUS = "focus"
    const val WEEKLY_INSIGHTS = "weekly_insights"
    const val COMMITMENT_SETUP = "commitment_setup"
    const val WEB_FILTER_DASHBOARD = "web_filter_dashboard"
    const val DOMAIN_BLACKLIST = "domain_blacklist"
    const val KEYWORD_MANAGER = "keyword_manager"
    const val URL_VISIT_LOG = "url_visit_log"
    const val WHITELIST = "whitelist"
    const val UNBLOCK_REQUEST = "unblock_request/{domain}"

    fun unblockRequest(domain: String) = "unblock_request/$domain"
}
