package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for `GET /control/status`.
 */
@Serializable
data class ServerStatusDto(
    @SerialName("dns_addresses")
    val dnsAddresses: List<String> = emptyList(),
    @SerialName("dns_port")
    val dnsPort: Int = 53,
    @SerialName("http_port")
    val httpPort: Int = 80,
    @SerialName("protection_enabled")
    val protectionEnabled: Boolean = false,
    @SerialName("protection_disabled_duration")
    val protectionDisabledDurationMillis: Long = 0L,
    @SerialName("running")
    val running: Boolean = false,
    @SerialName("version")
    val version: String = "",
    @SerialName("language")
    val language: String = "",
    @SerialName("dhcp_available")
    val dhcpAvailable: Boolean = false,
    @SerialName("start_time")
    val startTimeMillis: Double? = null,
)
