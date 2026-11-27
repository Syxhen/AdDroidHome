package com.syxhen.addroidhome.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for `GET /control/stats`.
 */
@Serializable
data class StatsDto(
    @SerialName("time_units")
    val timeUnits: String = "hours",
    @SerialName("num_dns_queries")
    val numDnsQueries: Long = 0L,
    @SerialName("num_blocked_filtering")
    val numBlockedFiltering: Long = 0L,
    @SerialName("num_replaced_safebrowsing")
    val numReplacedSafeBrowsing: Long = 0L,
    @SerialName("num_replaced_safesearch")
    val numReplacedSafeSearch: Long = 0L,
    @SerialName("num_replaced_parental")
    val numReplacedParental: Long = 0L,
    @SerialName("avg_processing_time")
    val avgProcessingTimeSeconds: Double = 0.0,
    @SerialName("dns_queries")
    val dnsQueries: List<Long> = emptyList(),
    @SerialName("blocked_filtering")
    val blockedFiltering: List<Long> = emptyList(),
    @SerialName("replaced_safebrowsing")
    val replacedSafeBrowsing: List<Long> = emptyList(),
    @SerialName("replaced_parental")
    val replacedParental: List<Long> = emptyList(),
    @SerialName("top_queried_domains")
    val topQueriedDomains: List<Map<String, Long>> = emptyList(),
    @SerialName("top_blocked_domains")
    val topBlockedDomains: List<Map<String, Long>> = emptyList(),
    @SerialName("top_clients")
    val topClients: List<Map<String, Long>> = emptyList(),
    @SerialName("top_upstreams_responses")
    val topUpstreamsResponses: List<Map<String, Long>> = emptyList(),
    @SerialName("top_upstreams_avg_time")
    val topUpstreamsAvgTime: List<Map<String, Double>> = emptyList(),
) {
    val avgProcessingTimeMillis: Double
        get() = avgProcessingTimeSeconds * 1_000.0

    val blockedPercentage: Double
        get() = if (numDnsQueries == 0L) {
            0.0
        } else {
            (numBlockedFiltering.toDouble() / numDnsQueries.toDouble()) * 100.0
        }
}

data class TopCountEntry(
    val name: String,
    val count: Long,
)

data class TopLatencyEntry(
    val name: String,
    val avgTimeSeconds: Double,
) {
    val avgTimeMillis: Double
        get() = avgTimeSeconds * 1_000.0
}

fun List<Map<String, Long>>.toTopCountEntries(): List<TopCountEntry> =
    flatMap { entry -> entry.entries.map { (name, count) -> TopCountEntry(name, count) } }

fun List<Map<String, Double>>.toTopLatencyEntries(): List<TopLatencyEntry> =
    flatMap { entry -> entry.entries.map { (name, seconds) -> TopLatencyEntry(name, seconds) } }
