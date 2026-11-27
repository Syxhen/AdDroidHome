package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /control/protection`.
 *
 * When [enabled] is `false`, [durationMillis] can optionally specify how long protection
 * remains paused (in milliseconds) before automatically re-enabling.
 */
@Serializable
data class SetProtectionRequest(
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("duration")
    val durationMillis: Long? = null,
)
