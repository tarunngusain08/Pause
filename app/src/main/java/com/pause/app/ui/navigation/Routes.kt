package com.pause.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val FOCUS_SETUP = "focus_setup"
    const val SOCIAL_FILTER = "social_filter"
    const val UNBLOCK_REQUEST = "unblock_request/{domain}"

    fun unblockRequest(domain: String) = "unblock_request/$domain"
}
