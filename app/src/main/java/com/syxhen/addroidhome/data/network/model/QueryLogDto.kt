package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Response model for `GET /control/querylog`.
 */
@Serializable
data class QueryLogResponseDto(
    @SerialName("oldest")
    val oldest: String = "",
    @SerialName("data")
    val data: List<QueryLogItemDto> = emptyList(),
)

/**
 * Single DNS query log entry matching AdGuard Home's `QueryLogItem` schema
 * (including backward-compatible `rule` / `filterId` fields for pre-v1.0.0 servers).
 */
@Serializable
data class QueryLogItemDto(
    @SerialName("time")
    val time: String = "",
    @SerialName("question")
    val question: DnsQuestionDto = DnsQuestionDto(),
    @SerialName("answer")
    val answer: List<DnsAnswerDto> = emptyList(),
    @SerialName("original_answer")
    val originalAnswer: List<DnsAnswerDto> = emptyList(),
    @SerialName("cached")
    val cached: Boolean = false,
    @SerialName("upstream")
    val upstream: String = "",
    @SerialName("answer_dnssec")
    val answerDnssec: Boolean = false,
    @SerialName("client")
    val client: String = "",
    @SerialName("client_id")
    val clientId: String = "",
    @SerialName("client_info")
    val clientInfo: QueryLogClientInfoDto? = null,
    @SerialName("client_proto")
    val clientProto: String = "",
    @SerialName("ecs")
    val ecs: String = "",
    @SerialName("elapsedMs")
    val elapsedMs: String = "",
    @SerialName("reason")
    val reason: String = "",
    @SerialName("rules")
    val rules: List<ResultRuleDto> = emptyList(),
    @SerialName("rule")
    val legacyRule: String? = null,
    @SerialName("filterId")
    val legacyFilterId: Long? = null,
    @SerialName("service_name")
    val serviceName: String = "",
    @SerialName("status")
    val status: String = "",
) {
    val displayDomain: String
        get() = question.unicodeName.ifBlank { question.name }.trimEnd('.')

    val clientDisplayName: String
        get() = clientInfo?.name?.takeIf { it.isNotBlank() }
            ?: clientId.takeIf { it.isNotBlank() }
            ?: client

    val effectiveRules: List<ResultRuleDto>
        get() = when {
            rules.isNotEmpty() -> rules
            !legacyRule.isNullOrBlank() -> listOf(
                ResultRuleDto(
                    filterListId = legacyFilterId ?: 0L,
                    text = legacyRule,
                ),
            )
            else -> emptyList()
        }

    val isBlocked: Boolean
        get() = reason in BLOCKED_REASONS

    val isWhitelisted: Boolean
        get() = reason == REASON_WHITELIST

    val isRewritten: Boolean
        get() = reason in REWRITTEN_REASONS

    val isBlockedByCustomRuleOnly: Boolean
        get() = isBlocked &&
            effectiveRules.isNotEmpty() &&
            effectiveRules.all { it.filterListId == FILTER_ID_CUSTOM_RULES }

    val formattedElapsedMs: String
        get() {
            val numeric = elapsedMs.toDoubleOrNull() ?: return elapsedMs
            return if (numeric < 10.0) {
                String.format(Locale.US, "%.2f ms", numeric)
            } else {
                String.format(Locale.US, "%.1f ms", numeric)
            }
        }

    val reasonLabel: String
        get() = when (reason) {
            "NotFilteredNotFound" -> "Processed"
            REASON_WHITELIST -> "Allowed (Rule)"
            "NotFilteredError" -> "Error"
            "FilteredBlackList" -> "Blocked"
            "FilteredSafeBrowsing" -> "Safe Browsing"
            "FilteredParental" -> "Parental Control"
            "FilteredInvalid" -> "Invalid"
            "FilteredSafeSearch" -> "Safe Search"
            "FilteredBlockedService" -> if (serviceName.isNotBlank()) "Service: $serviceName" else "Blocked Service"
            "Rewrite" -> "Rewritten"
            "RewriteEtcHosts" -> "Hosts Rewrite"
            "RewriteRule" -> "Rule Rewrite"
            else -> reason.ifBlank { "Processed" }
        }

    companion object {
        const val FILTER_ID_CUSTOM_RULES = 0L
        const val REASON_WHITELIST = "NotFilteredWhiteList"

        val BLOCKED_REASONS = setOf(
            "FilteredBlackList",
            "FilteredSafeBrowsing",
            "FilteredParental",
            "FilteredBlockedService",
            "FilteredInvalid",
        )

        val REWRITTEN_REASONS = setOf(
            "Rewrite",
            "RewriteEtcHosts",
            "RewriteRule",
            "FilteredSafeSearch",
        )
    }
}

@Serializable
data class DnsQuestionDto(
    @SerialName("name")
    val name: String = "",
    @SerialName("unicode_name")
    val unicodeName: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("class")
    val dnsClass: String = "",
)

@Serializable
data class DnsAnswerDto(
    @SerialName("type")
    val type: String = "",
    @SerialName("value")
    val value: String = "",
    @SerialName("ttl")
    val ttl: Long = 0L,
)

@Serializable
data class ResultRuleDto(
    @SerialName("filter_list_id")
    val filterListId: Long = 0L,
    @SerialName("text")
    val text: String = "",
) {
    val sourceLabel: String
        get() = when (filterListId) {
            0L -> "Custom filtering rules"
            -1L -> "System hosts file"
            -2L -> "Blocked services"
            -3L -> "Parental control"
            -4L -> "Safe browsing"
            -5L -> "Safe search"
            else -> "Filter list #$filterListId"
        }
}

@Serializable
data class QueryLogClientInfoDto(
    @SerialName("name")
    val name: String = "",
    @SerialName("disallowed")
    val disallowed: Boolean = false,
    @SerialName("disallowed_rule")
    val disallowedRule: String = "",
    @SerialName("whois")
    val whois: QueryLogClientWhoisDto? = null,
)

@Serializable
data class QueryLogClientWhoisDto(
    @SerialName("city")
    val city: String = "",
    @SerialName("country")
    val country: String = "",
    @SerialName("orgname")
    val orgName: String = "",
)

/**
 * Filter presets for `GET /control/querylog`.
 *
 * AdGuard Home v0.107.77+ / v1.0.0 uses repeated `reason` query parameters, while
 * earlier versions used `response_status`.
 */
enum class QueryLogFilter(
    val label: String,
    val reasons: List<String>?,
    val legacyResponseStatus: String?,
) {
    All(
        label = "All",
        reasons = null,
        legacyResponseStatus = "all",
    ),
    Blocked(
        label = "Blocked",
        reasons = listOf(
            "FilteredBlackList",
            "FilteredSafeBrowsing",
            "FilteredParental",
            "FilteredBlockedService",
            "FilteredInvalid",
        ),
        legacyResponseStatus = "blocked",
    ),
    SafeAndParental(
        label = "Safe & Parental",
        reasons = listOf(
            "FilteredSafeBrowsing",
            "FilteredParental",
            "FilteredSafeSearch",
        ),
        legacyResponseStatus = "blocked",
    ),
    Allowed(
        label = "Allowed",
        reasons = listOf("NotFilteredNotFound", "NotFilteredWhiteList"),
        legacyResponseStatus = "processed",
    ),
    Whitelisted(
        label = "Whitelisted",
        reasons = listOf("NotFilteredWhiteList"),
        legacyResponseStatus = "whitelisted",
    ),
    Rewritten(
        label = "Rewritten",
        reasons = listOf("Rewrite", "RewriteEtcHosts", "RewriteRule", "FilteredSafeSearch"),
        legacyResponseStatus = "rewritten",
    ),
}
