package com.syxhen.addroidhome.data.network.api

import com.syxhen.addroidhome.data.network.model.AddFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.BlockedServicesAllDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.EnabledStatusDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogResponseDto
import com.syxhen.addroidhome.data.network.model.RefreshFiltersRequest
import com.syxhen.addroidhome.data.network.model.RefreshFiltersResponse
import com.syxhen.addroidhome.data.network.model.RemoveFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.RewriteDeleteRequest
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.RewriteUpdateRequest
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.SetFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringConfigRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringRulesRequest
import com.syxhen.addroidhome.data.network.model.SetProtectionRequest
import com.syxhen.addroidhome.data.network.model.StatsDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Retrofit interface for the AdGuard Home `/control` HTTP API.
 *
 * Paths intentionally omit a leading slash so reverse-proxy subpaths
 * (e.g., `https://home.example.com/adguard/`) are preserved when resolving
 * against the Retrofit base URL.
 */
interface AdGuardHomeApi {

    /**
     * Retrieves current DNS server status and general configuration.
     */
    @GET("control/status")
    suspend fun getStatus(): ServerStatusDto

    /**
     * Retrieves DNS server statistics.
     *
     * @param recentMillis Optional lookback period in milliseconds (must be a multiple of one hour
     *   and not exceed the server's configured `statistics.interval`; supported in AdGuard Home v0.107.72+).
     */
    @GET("control/stats")
    suspend fun getStats(
        @Query("recent") recentMillis: Long? = null,
    ): StatsDto

    /**
     * Enables or disables DNS protection, with an optional pause duration.
     */
    @POST("control/protection")
    suspend fun setProtection(
        @Body request: SetProtectionRequest,
    )

    /**
     * Retrieves DNS query log entries.
     *
     * Uses `reason` (repeated query parameter) on AdGuard Home v0.107.77+ / v1.0.0+, or
     * legacy `response_status` on earlier servers.
     */
    @GET("control/querylog")
    suspend fun getQueryLog(
        @Query("search") search: String? = null,
        @Query("older_than") olderThan: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("reason") reasons: List<String>? = null,
        @Query("response_status") responseStatus: String? = null,
    ): QueryLogResponseDto

    /**
     * Retrieves DNS filtering settings, filter lists, and custom user rules.
     */
    @GET("control/filtering/status")
    suspend fun getFilteringStatus(): FilterStatusDto

    /**
     * Updates global DNS filtering enabled flag and list auto-update interval.
     */
    @POST("control/filtering/config")
    suspend fun setFilteringConfig(
        @Body request: SetFilteringConfigRequest,
    )

    /**
     * Updates properties (enabled state, name, URL) of an existing blocklist or allowlist.
     */
    @POST("control/filtering/set_url")
    suspend fun setFilterUrl(
        @Body request: SetFilterUrlRequest,
    )

    /**
     * Adds a new blocklist or allowlist by URL.
     *
     * AdGuard Home responds with plain text (`OK <n> rules\n`), so the response is read as
     * [ResponseBody] rather than JSON.
     */
    @POST("control/filtering/add_url")
    suspend fun addFilterUrl(
        @Body request: AddFilterUrlRequest,
    ): ResponseBody

    /**
     * Removes an existing blocklist or allowlist by URL.
     *
     * AdGuard Home responds with plain text (`OK <n> rules\n`), so the response is read as
     * [ResponseBody] rather than JSON.
     */
    @POST("control/filtering/remove_url")
    suspend fun removeFilterUrl(
        @Body request: RemoveFilterUrlRequest,
    ): ResponseBody

    /**
     * Forces AdGuard Home to download updates for blocklists (`whitelist = false`)
     * or allowlists (`whitelist = true`).
     */
    @POST("control/filtering/refresh")
    suspend fun refreshFilters(
        @Body request: RefreshFiltersRequest,
    ): RefreshFiltersResponse

    /**
     * Replaces custom filtering rules (`user_rules`) on the AdGuard Home server.
     */
    @POST("control/filtering/set_rules")
    suspend fun setFilteringRules(
        @Body request: SetFilteringRulesRequest,
    )

    /**
     * Retrieves the catalog of all blockable services and their groups.
     */
    @GET("control/blocked_services/all")
    suspend fun getAllBlockedServices(): BlockedServicesAllDto

    /**
     * Retrieves currently blocked service IDs and their pause schedule.
     */
    @GET("control/blocked_services/get")
    suspend fun getBlockedServices(): BlockedServicesScheduleDto

    /**
     * Updates blocked service IDs while preserving or updating the weekly pause schedule.
     */
    @PUT("control/blocked_services/update")
    suspend fun updateBlockedServices(
        @Body request: BlockedServicesScheduleDto,
    )

    /**
     * Retrieves AdGuard Safe Browsing status.
     */
    @GET("control/safebrowsing/status")
    suspend fun getSafeBrowsingStatus(): EnabledStatusDto

    /**
     * Enables AdGuard Safe Browsing.
     */
    @POST("control/safebrowsing/enable")
    suspend fun enableSafeBrowsing()

    /**
     * Disables AdGuard Safe Browsing.
     */
    @POST("control/safebrowsing/disable")
    suspend fun disableSafeBrowsing()

    /**
     * Retrieves AdGuard Parental Control status.
     */
    @GET("control/parental/status")
    suspend fun getParentalStatus(): EnabledStatusDto

    /**
     * Enables AdGuard Parental Control.
     */
    @POST("control/parental/enable")
    suspend fun enableParental()

    /**
     * Disables AdGuard Parental Control.
     */
    @POST("control/parental/disable")
    suspend fun disableParental()

    /**
     * Retrieves Safe Search status and per-engine configuration.
     */
    @GET("control/safesearch/status")
    suspend fun getSafeSearchStatus(): SafeSearchConfigDto

    /**
     * Updates Safe Search master switch and per-engine flags.
     */
    @PUT("control/safesearch/settings")
    suspend fun updateSafeSearchSettings(
        @Body request: SafeSearchConfigDto,
    )

    /**
     * Retrieves configured DNS rewrite entries.
     */
    @GET("control/rewrite/list")
    suspend fun getRewriteList(): List<RewriteEntryDto>

    /**
     * Retrieves global DNS rewrites enabled flag (AdGuard Home schema v31+).
     */
    @GET("control/rewrite/settings")
    suspend fun getRewriteSettings(): EnabledStatusDto

    /**
     * Updates global DNS rewrites enabled flag (AdGuard Home schema v31+).
     */
    @PUT("control/rewrite/settings/update")
    suspend fun updateRewriteSettings(
        @Body request: EnabledStatusDto,
    )

    /**
     * Adds a DNS rewrite entry.
     */
    @POST("control/rewrite/add")
    suspend fun addRewrite(
        @Body request: RewriteEntryDto,
    )

    /**
     * Deletes a DNS rewrite entry.
     */
    @POST("control/rewrite/delete")
    suspend fun deleteRewrite(
        @Body request: RewriteDeleteRequest,
    )

    /**
     * Updates an existing DNS rewrite entry (domain, answer, or enabled flag).
     */
    @PUT("control/rewrite/update")
    suspend fun updateRewrite(
        @Body request: RewriteUpdateRequest,
    )

    /**
     * Clears all stored DNS query log entries on the server.
     */
    @POST("control/querylog_clear")
    suspend fun clearQueryLog()

    /**
     * Resets all DNS statistics counters to zero on the server.
     */
    @POST("control/stats_reset")
    suspend fun resetStats()
}
