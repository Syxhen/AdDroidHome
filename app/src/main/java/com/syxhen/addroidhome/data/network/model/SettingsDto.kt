package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared `{ "enabled": boolean }` payload used by Safe Browsing, Parental Control,
 * and global DNS Rewrites settings endpoints.
 */
@Serializable
data class EnabledStatusDto(
    @SerialName("enabled")
    val enabled: Boolean = false,
)

/**
 * Response and update body for `GET /control/safesearch/status` and
 * `PUT /control/safesearch/settings`.
 */
@Serializable
data class SafeSearchConfigDto(
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("bing")
    val bing: Boolean = false,
    @SerialName("duckduckgo")
    val duckDuckGo: Boolean = false,
    @SerialName("ecosia")
    val ecosia: Boolean = false,
    @SerialName("google")
    val google: Boolean = false,
    @SerialName("pixabay")
    val pixabay: Boolean = false,
    @SerialName("yandex")
    val yandex: Boolean = false,
    @SerialName("youtube")
    val youTube: Boolean = false,
) {
    fun isEngineEnabled(engine: SafeSearchEngine): Boolean = when (engine) {
        SafeSearchEngine.Google -> google
        SafeSearchEngine.Bing -> bing
        SafeSearchEngine.YouTube -> youTube
        SafeSearchEngine.DuckDuckGo -> duckDuckGo
        SafeSearchEngine.Ecosia -> ecosia
        SafeSearchEngine.Pixabay -> pixabay
        SafeSearchEngine.Yandex -> yandex
    }

    fun withEngineEnabled(engine: SafeSearchEngine, value: Boolean): SafeSearchConfigDto =
        when (engine) {
            SafeSearchEngine.Google -> copy(google = value)
            SafeSearchEngine.Bing -> copy(bing = value)
            SafeSearchEngine.YouTube -> copy(youTube = value)
            SafeSearchEngine.DuckDuckGo -> copy(duckDuckGo = value)
            SafeSearchEngine.Ecosia -> copy(ecosia = value)
            SafeSearchEngine.Pixabay -> copy(pixabay = value)
            SafeSearchEngine.Yandex -> copy(yandex = value)
        }

    val enabledEngineCount: Int
        get() = SafeSearchEngine.entries.count { isEngineEnabled(it) }

    fun withAllEnginesEnabled(): SafeSearchConfigDto = copy(
        bing = true,
        duckDuckGo = true,
        ecosia = true,
        google = true,
        pixabay = true,
        yandex = true,
        youTube = true,
    )
}

enum class SafeSearchEngine(val label: String) {
    Google("Google"),
    Bing("Bing"),
    YouTube("YouTube"),
    DuckDuckGo("DuckDuckGo"),
    Ecosia("Ecosia"),
    Pixabay("Pixabay"),
    Yandex("Yandex"),
}

/**
 * Single DNS rewrite entry returned by `GET /control/rewrite/list` and sent to
 * `POST /control/rewrite/add` or inside `PUT /control/rewrite/update`.
 *
 * `enabled` defaults to `true` for backward compatibility with older AdGuard Home servers
 * prior to config schema v31.
 */
@Serializable
data class RewriteEntryDto(
    @SerialName("domain")
    val domain: String = "",
    @SerialName("answer")
    val answer: String = "",
    @SerialName("enabled")
    val enabled: Boolean = true,
)

/**
 * Request body for `POST /control/rewrite/delete`.
 */
@Serializable
data class RewriteDeleteRequest(
    @SerialName("domain")
    val domain: String,
    @SerialName("answer")
    val answer: String,
)

/**
 * Request body for `PUT /control/rewrite/update`.
 */
@Serializable
data class RewriteUpdateRequest(
    @SerialName("target")
    val target: RewriteEntryDto,
    @SerialName("update")
    val update: RewriteEntryDto,
)
