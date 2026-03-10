package com.pause.app.service.webfilter.url

import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts the current URL from browser accessibility nodes.
 * Maps known browser package names to their URL bar view IDs.
 */
@Singleton
class BrowserURLReader @Inject constructor() {

    private val browserUrlBarIds = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/url_bar",
        "org.mozilla.fennec_aurora" to "org.mozilla.fennec_aurora:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/url_bar",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.duckduckgo.mobile.android" to "com.duckduckgo.mobile.android:id/url_bar",
        "com.opera.browser" to "com.opera.browser:id/url_bar",
        "org.mozilla.focus" to "org.mozilla.focus:id/url_bar"
    )

    private val knownBrowsers = browserUrlBarIds.keys

    private val urlPattern = Pattern.compile(
        "https?://[^\\s]+",
        Pattern.CASE_INSENSITIVE
    )

    fun isKnownBrowser(packageName: String): Boolean =
        packageName in knownBrowsers

    fun extractURL(rootNode: AccessibilityNodeInfo?, packageName: String): String? {
        if (rootNode == null) return null
        val viewId = browserUrlBarIds[packageName]
        if (viewId != null) {
            val urlBar = findNodeByViewId(rootNode, viewId)
            if (urlBar != null) {
                val text = urlBar.text?.toString() ?: urlBar.contentDescription?.toString()
                if (text != null && isValidUrlLike(text)) return text.trim()
            }
        }
        return findUrlInEditTexts(rootNode)
    }

    private fun findNodeByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        if (root.viewIdResourceName == viewId) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByViewId(child, viewId)
            if (found != null) {
                // Recycle the child only if it is not the found node itself
                if (found !== child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun findUrlInEditTexts(root: AccessibilityNodeInfo): String? {
        if (root.className?.toString() == "android.widget.EditText") {
            val text = root.text?.toString() ?: root.contentDescription?.toString()
            if (text != null && isValidUrlLike(text)) return text.trim()
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findUrlInEditTexts(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun isValidUrlLike(text: String): Boolean {
        if (text.length < 10) return false
        return text.startsWith("http://") || text.startsWith("https://") ||
            text.contains(".") && urlPattern.matcher(text).find()
    }
}
