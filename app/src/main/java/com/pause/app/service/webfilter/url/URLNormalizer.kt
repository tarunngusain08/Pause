package com.pause.app.service.webfilter.url

import java.net.URI

/**
 * Normalizes URLs for consistent matching and domain extraction.
 */
object URLNormalizer {

    private val URL_REGEX = Regex(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
        RegexOption.IGNORE_CASE
    )

    fun normalize(rawUrl: String): String {
        return rawUrl
            .trim()
            .lowercase()
            .removePrefix("www.")
            .removeSuffix("/")
    }

    fun extractDomain(url: String): String? {
        return try {
            val withProtocol = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }
            val uri = URI(withProtocol)
            val host = uri.host ?: return null
            host.removePrefix("www.").lowercase()
        } catch (_: Exception) {
            null
        }
    }

    fun stripTracking(url: String): String {
        val trackingParams = setOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid"
        )
        return try {
            val uri = URI(url)
            val query = uri.query ?: return url
            val filtered = query.split("&")
                .filterNot { param ->
                    trackingParams.any { param.startsWith("$it=") }
                }
            val newQuery = filtered.joinToString("&")
            val path = uri.rawPath.ifEmpty { "/" }
            "${uri.scheme}://${uri.host}$path${if (newQuery.isNotEmpty()) "?$newQuery" else ""}"
        } catch (_: Exception) {
            url
        }
    }

    fun isValidURL(url: String): Boolean =
        URL_REGEX.matches(url) || url.contains(".")
}
