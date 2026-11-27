package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale

/**
 * Response model for `GET /control/blocked_services/all`.
 */
@Serializable
data class BlockedServicesAllDto(
    @SerialName("blocked_services")
    val blockedServices: List<BlockedServiceDto> = emptyList(),
    @SerialName("groups")
    val groups: List<ServiceGroupDto> = emptyList(),
)

/**
 * Catalog entry describing a blockable service in AdGuard Home.
 */
@Serializable
data class BlockedServiceDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("group_id")
    val groupId: String = "",
    @SerialName("icon_svg")
    val iconSvg: String = "",
    @SerialName("rules")
    val rules: List<String> = emptyList(),
) {
    val displayName: String
        get() = name.ifBlank { id }
}

/**
 * Category/group descriptor returned by `GET /control/blocked_services/all`.
 */
@Serializable
data class ServiceGroupDto(
    @SerialName("id")
    val id: String = "",
) {
    val displayLabel: String
        get() = formatServiceGroupLabel(id)
}

/**
 * Payload for `GET /control/blocked_services/get` and `PUT /control/blocked_services/update`.
 *
 * The `schedule` object is kept as a raw [JsonObject] so all weekly day pause windows
 * (`sun`..`sat`), `time_zone`, and any future server fields are preserved verbatim on update.
 */
@Serializable
data class BlockedServicesScheduleDto(
    @SerialName("ids")
    val ids: List<String> = emptyList(),
    @SerialName("schedule")
    val schedule: JsonObject? = null,
) {
    val scheduleTimeZone: String?
        get() = (schedule?.get("time_zone") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    val activeScheduleDaysCount: Int
        get() = DAY_KEYS.count { key ->
            val entry = schedule?.get(key)
            entry is JsonObject && entry.isNotEmpty()
        }

    val hasActiveSchedule: Boolean
        get() = activeScheduleDaysCount > 0

    companion object {
        private val DAY_KEYS = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
    }
}

fun formatServiceGroupLabel(groupId: String): String {
    if (groupId.isBlank()) return "Other"
    return when (groupId.lowercase(Locale.ROOT)) {
        "ai" -> "AI"
        "cdn" -> "CDN"
        "social_network" -> "Social Networks"
        "streaming" -> "Streaming"
        "gaming" -> "Gaming"
        "messenger" -> "Messengers"
        "shopping" -> "Shopping"
        "dating" -> "Dating"
        "gambling" -> "Gambling"
        "hosting" -> "Hosting"
        "piracy" -> "Piracy"
        "privacy" -> "Privacy"
        "software" -> "Software"
        else -> groupId
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
    }
}
