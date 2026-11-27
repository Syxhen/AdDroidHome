package com.syxhen.addroidhome.data.model

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class ServerConfig(
    val baseUrl: String,
    val username: String = "",
    val password: String = "",
    val trustSelfSignedCerts: Boolean = false,
) {
    val displayHost: String
        get() = runCatching {
            val uri = URI(baseUrl)
            val host = uri.host ?: return baseUrl
            if (uri.port != -1) "$host:${uri.port}" else host
        }.getOrDefault(baseUrl)

    companion object {
        fun normalizeBaseUrl(rawInput: String): String {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return ""

            val withScheme = if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }

            return withScheme
                .trimEnd('/')
                .removeSuffix("/control")
                .trimEnd('/')
        }

        fun isValidUrl(normalizedUrl: String): Boolean {
            if (normalizedUrl.isBlank()) return false
            return runCatching {
                val uri = URI(normalizedUrl)
                (uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)) &&
                    !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
    }
}
