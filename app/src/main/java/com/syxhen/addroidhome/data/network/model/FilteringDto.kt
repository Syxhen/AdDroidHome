package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for `GET /control/filtering/status`.
 */
@Serializable
data class FilterStatusDto(
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("interval")
    val intervalHours: Int = 24,
    @SerialName("filters")
    val filters: List<FilterListDto> = emptyList(),
    @SerialName("whitelist_filters")
    val whitelistFilters: List<FilterListDto> = emptyList(),
    @SerialName("user_rules")
    val userRules: List<String> = emptyList(),
) {
    val enabledBlocklistsCount: Int
        get() = filters.count { it.enabled }

    val enabledAllowlistsCount: Int
        get() = whitelistFilters.count { it.enabled }

    val activeBlocklistRulesCount: Long
        get() = filters.filter { it.enabled }.sumOf { it.rulesCount }

    val activeAllowlistRulesCount: Long
        get() = whitelistFilters.filter { it.enabled }.sumOf { it.rulesCount }
}

@Serializable
data class FilterListDto(
    @SerialName("id")
    val id: Long = 0L,
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("url")
    val url: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("rules_count")
    val rulesCount: Long = 0L,
    @SerialName("last_updated")
    val lastUpdated: String = "",
)

/**
 * Request body for `POST /control/filtering/set_rules`.
 */
@Serializable
data class SetFilteringRulesRequest(
    @SerialName("rules")
    val rules: List<String>,
)

/**
 * Nested payload for `POST /control/filtering/set_url`.
 */
@Serializable
data class FilterUrlDataDto(
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
    @SerialName("enabled")
    val enabled: Boolean,
)

/**
 * Request body for `POST /control/filtering/set_url`.
 */
@Serializable
data class SetFilterUrlRequest(
    @SerialName("url")
    val url: String,
    @SerialName("whitelist")
    val whitelist: Boolean,
    @SerialName("data")
    val data: FilterUrlDataDto,
)

/**
 * Request body for `POST /control/filtering/add_url`.
 */
@Serializable
data class AddFilterUrlRequest(
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
    @SerialName("whitelist")
    val whitelist: Boolean,
)

/**
 * Request body for `POST /control/filtering/remove_url`.
 */
@Serializable
data class RemoveFilterUrlRequest(
    @SerialName("url")
    val url: String,
    @SerialName("whitelist")
    val whitelist: Boolean,
)

/**
 * Request body for `POST /control/filtering/refresh`.
 */
@Serializable
data class RefreshFiltersRequest(
    @SerialName("whitelist")
    val whitelist: Boolean,
)

/**
 * Response model for `POST /control/filtering/refresh`.
 */
@Serializable
data class RefreshFiltersResponse(
    @SerialName("updated")
    val updated: Int = 0,
)

/**
 * Request body for `POST /control/filtering/config`.
 */
@Serializable
data class SetFilteringConfigRequest(
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("interval")
    val intervalHours: Int,
)
