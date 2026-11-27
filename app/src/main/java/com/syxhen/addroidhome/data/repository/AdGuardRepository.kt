package com.syxhen.addroidhome.data.repository

import com.syxhen.addroidhome.data.model.CustomRuleUpdater
import com.syxhen.addroidhome.data.model.DashboardSnapshot
import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.AdGuardNetworkFactory
import com.syxhen.addroidhome.data.network.LocalNetworkPolicy
import com.syxhen.addroidhome.data.network.api.AdGuardHomeApi
import com.syxhen.addroidhome.data.network.model.AddFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.BlockedServiceDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.EnabledStatusDto
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.FilterUrlDataDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogResponseDto
import com.syxhen.addroidhome.data.network.model.RefreshFiltersRequest
import com.syxhen.addroidhome.data.network.model.RemoveFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.RewriteDeleteRequest
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.RewriteUpdateRequest
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.ServiceGroupDto
import com.syxhen.addroidhome.data.network.model.SetFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringConfigRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringRulesRequest
import com.syxhen.addroidhome.data.network.model.SetProtectionRequest
import com.syxhen.addroidhome.data.storage.ServerConfigStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class FilterRefreshResult(
    val updatedCount: Int,
    val status: FilterStatusDto,
)

data class BlockedServicesSnapshot(
    val services: List<BlockedServiceDto>,
    val groups: List<ServiceGroupDto>,
    val current: BlockedServicesScheduleDto,
)

data class SettingsSnapshot(
    val safeBrowsingEnabled: Boolean,
    val parentalEnabled: Boolean,
    val safeSearch: SafeSearchConfigDto,
    val rewrites: List<RewriteEntryDto>,
    val rewritesEnabled: Boolean? = null,
)

interface AdGuardRepository {
    fun getSavedConfig(): ServerConfig?
    suspend fun connectAndLoadDashboard(config: ServerConfig): Result<DashboardSnapshot>
    suspend fun refreshDashboard(recentMillis: Long? = null): Result<DashboardSnapshot>
    suspend fun setProtection(
        enabled: Boolean,
        durationMillis: Long? = null,
        recentMillis: Long? = null,
    ): Result<DashboardSnapshot>
    suspend fun getQueryLog(
        search: String? = null,
        filter: QueryLogFilter = QueryLogFilter.All,
        olderThan: String? = null,
        limit: Int = DEFAULT_QUERY_LOG_PAGE_SIZE,
    ): Result<QueryLogResponseDto>

    suspend fun setDomainBlocked(
        domain: String,
        block: Boolean,
        blockedByCustomRuleOnly: Boolean = false,
    ): Result<Unit>

    suspend fun getFilteringStatus(): Result<FilterStatusDto>

    suspend fun setFilteringEnabled(
        enabled: Boolean,
        intervalHours: Int,
    ): Result<FilterStatusDto>

    suspend fun setFilterListEnabled(
        filter: FilterListDto,
        whitelist: Boolean,
        enabled: Boolean,
    ): Result<FilterStatusDto>

    suspend fun addFilterList(
        name: String,
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto>

    suspend fun removeFilterList(
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto>

    suspend fun refreshFilterLists(whitelist: Boolean): Result<FilterRefreshResult>

    suspend fun setCustomRules(rules: List<String>): Result<FilterStatusDto>

    suspend fun getBlockedServicesSnapshot(): Result<BlockedServicesSnapshot>

    suspend fun updateBlockedServices(blockedIds: Set<String>): Result<BlockedServicesScheduleDto>

    suspend fun getSettingsSnapshot(): Result<SettingsSnapshot>

    suspend fun setSafeBrowsingEnabled(enabled: Boolean): Result<Boolean>

    suspend fun setParentalEnabled(enabled: Boolean): Result<Boolean>

    suspend fun updateSafeSearch(config: SafeSearchConfigDto): Result<SafeSearchConfigDto>

    suspend fun setRewritesEnabled(enabled: Boolean): Result<Boolean>

    suspend fun addRewrite(
        domain: String,
        answer: String,
        enabled: Boolean = true,
    ): Result<List<RewriteEntryDto>>

    suspend fun updateRewrite(
        target: RewriteEntryDto,
        updated: RewriteEntryDto,
    ): Result<List<RewriteEntryDto>>

    suspend fun deleteRewrite(entry: RewriteEntryDto): Result<List<RewriteEntryDto>>

    suspend fun clearQueryLog(): Result<Unit>

    suspend fun resetStats(): Result<Unit>

    fun disconnect()

    companion object {
        const val DEFAULT_QUERY_LOG_PAGE_SIZE = 50
    }
}

class DefaultAdGuardRepository(
    private val storage: ServerConfigStorage,
    private val apiFactory: (ServerConfig) -> AdGuardHomeApi = { config ->
        AdGuardNetworkFactory.createApi(
            baseUrl = config.baseUrl,
            username = config.username.ifBlank { null },
            password = if (config.username.isBlank()) null else config.password,
            trustSelfSignedCerts = config.trustSelfSignedCerts,
        )
    },
) : AdGuardRepository {

    @Volatile
    private var activeConfig: ServerConfig? = null

    @Volatile
    private var activeApi: AdGuardHomeApi? = null

    @Volatile
    private var lastServerVersion: String = ""

    override fun getSavedConfig(): ServerConfig? = storage.load()

    override suspend fun connectAndLoadDashboard(config: ServerConfig): Result<DashboardSnapshot> {
        val normalizedUrl = ServerConfig.normalizeBaseUrl(config.baseUrl)
        if (!ServerConfig.isValidUrl(normalizedUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid http:// or https:// server URL."))
        }
        if (!LocalNetworkPolicy.isCleartextAllowedForUrl(normalizedUrl)) {
            return Result.failure(
                IllegalArgumentException(
                    "Plain HTTP is only allowed for local/LAN servers. Use https:// for public addresses.",
                ),
            )
        }
        val normalizedConfig = config.copy(
            baseUrl = normalizedUrl,
            username = config.username.trim(),
        )
        val candidateApi = runCatching { apiFactory(normalizedConfig) }.getOrElse { error ->
            return Result.failure(IllegalArgumentException(mapErrorMessage(error), error))
        }

        return fetchSnapshot(candidateApi).onSuccess { snapshot ->
            activeConfig = normalizedConfig
            activeApi = candidateApi
            lastServerVersion = snapshot.status.version
            storage.save(normalizedConfig)
        }
    }

    override suspend fun refreshDashboard(recentMillis: Long?): Result<DashboardSnapshot> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return fetchSnapshot(api, recentMillis = recentMillis).onSuccess { snapshot ->
            lastServerVersion = snapshot.status.version
        }
    }

    override suspend fun setProtection(
        enabled: Boolean,
        durationMillis: Long?,
        recentMillis: Long?,
    ): Result<DashboardSnapshot> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return try {
            api.setProtection(
                SetProtectionRequest(
                    enabled = enabled,
                    durationMillis = if (enabled) null else durationMillis,
                ),
            )
            fetchSnapshot(api, recentMillis = recentMillis)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AdGuardApiException(mapErrorMessage(e), e))
        }
    }

    override suspend fun getQueryLog(
        search: String?,
        filter: QueryLogFilter,
        olderThan: String?,
        limit: Int,
    ): Result<QueryLogResponseDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        val normalizedSearch = search?.trim()?.ifBlank { null }
        val normalizedOlderThan = olderThan?.ifBlank { null }
        val useReasonParam = supportsReasonQueryParam(lastServerVersion)

        return try {
            val response = if (filter == QueryLogFilter.All) {
                api.getQueryLog(
                    search = normalizedSearch,
                    olderThan = normalizedOlderThan,
                    limit = limit,
                )
            } else if (useReasonParam) {
                api.getQueryLog(
                    search = normalizedSearch,
                    olderThan = normalizedOlderThan,
                    limit = limit,
                    reasons = filter.reasons,
                )
            } else {
                api.getQueryLog(
                    search = normalizedSearch,
                    olderThan = normalizedOlderThan,
                    limit = limit,
                    responseStatus = filter.legacyResponseStatus,
                )
            }
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AdGuardApiException(mapErrorMessage(e), e))
        }
    }

    override suspend fun setDomainBlocked(
        domain: String,
        block: Boolean,
        blockedByCustomRuleOnly: Boolean,
    ): Result<Unit> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return try {
            val currentStatus = api.getFilteringStatus()
            val updatedRules = CustomRuleUpdater.applyQuickRule(
                existingRules = currentStatus.userRules,
                rawDomain = domain,
                block = block,
                blockedByCustomRuleOnly = blockedByCustomRuleOnly,
            )
            api.setFilteringRules(SetFilteringRulesRequest(rules = updatedRules))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AdGuardApiException(mapErrorMessage(e), e))
        }
    }

    override suspend fun getFilteringStatus(): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall { api.getFilteringStatus() }
    }

    override suspend fun setFilteringEnabled(
        enabled: Boolean,
        intervalHours: Int,
    ): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.setFilteringConfig(
                SetFilteringConfigRequest(
                    enabled = enabled,
                    intervalHours = intervalHours,
                ),
            )
            api.getFilteringStatus()
        }
    }

    override suspend fun setFilterListEnabled(
        filter: FilterListDto,
        whitelist: Boolean,
        enabled: Boolean,
    ): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.setFilterUrl(
                SetFilterUrlRequest(
                    url = filter.url,
                    whitelist = whitelist,
                    data = FilterUrlDataDto(
                        name = filter.name,
                        url = filter.url,
                        enabled = enabled,
                    ),
                ),
            )
            api.getFilteringStatus()
        }
    }

    override suspend fun addFilterList(
        name: String,
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        val trimmedName = name.trim()
        val trimmedUrl = url.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a name for the filter list."))
        }
        if (trimmedUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a filter list URL or absolute file path."))
        }
        val isHttpOrPath = trimmedUrl.startsWith("https://", ignoreCase = true) ||
            trimmedUrl.startsWith("http://", ignoreCase = true) ||
            trimmedUrl.startsWith("/")
        if (!isHttpOrPath) {
            return Result.failure(IllegalArgumentException("Filter URL must start with https://, http://, or /."))
        }

        return executeApiCall {
            api.addFilterUrl(
                AddFilterUrlRequest(
                    name = trimmedName,
                    url = trimmedUrl,
                    whitelist = whitelist,
                ),
            ).close()
            api.getFilteringStatus()
        }
    }

    override suspend fun removeFilterList(
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.removeFilterUrl(
                RemoveFilterUrlRequest(
                    url = url,
                    whitelist = whitelist,
                ),
            ).close()
            api.getFilteringStatus()
        }
    }

    override suspend fun refreshFilterLists(whitelist: Boolean): Result<FilterRefreshResult> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            val refreshResponse = api.refreshFilters(RefreshFiltersRequest(whitelist = whitelist))
            val updatedStatus = api.getFilteringStatus()
            FilterRefreshResult(
                updatedCount = refreshResponse.updated,
                status = updatedStatus,
            )
        }
    }

    override suspend fun setCustomRules(rules: List<String>): Result<FilterStatusDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.setFilteringRules(SetFilteringRulesRequest(rules = rules))
            api.getFilteringStatus()
        }
    }

    override suspend fun getBlockedServicesSnapshot(): Result<BlockedServicesSnapshot> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            coroutineScope {
                val allDeferred = async { api.getAllBlockedServices() }
                val currentDeferred = async { api.getBlockedServices() }
                val all = allDeferred.await()
                val current = currentDeferred.await()
                BlockedServicesSnapshot(
                    services = all.blockedServices.sortedBy { it.displayName.lowercase() },
                    groups = all.groups,
                    current = current,
                )
            }
        }
    }

    override suspend fun updateBlockedServices(
        blockedIds: Set<String>,
    ): Result<BlockedServicesScheduleDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            // Re-read current server schedule right before PUT so any pause schedule
            // configured on the server is always preserved verbatim.
            val latest = api.getBlockedServices()
            val payload = BlockedServicesScheduleDto(
                ids = blockedIds.sorted(),
                schedule = latest.schedule,
            )
            api.updateBlockedServices(payload)
            api.getBlockedServices()
        }
    }

    override suspend fun getSettingsSnapshot(): Result<SettingsSnapshot> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            coroutineScope {
                val safeBrowsingDeferred = async { api.getSafeBrowsingStatus() }
                val parentalDeferred = async { api.getParentalStatus() }
                val safeSearchDeferred = async { api.getSafeSearchStatus() }
                val rewritesDeferred = async { api.getRewriteList() }
                val rewriteSettingsDeferred = async {
                    try {
                        api.getRewriteSettings().enabled
                    } catch (e: HttpException) {
                        if (e.code() == 404) null else throw e
                    }
                }
                SettingsSnapshot(
                    safeBrowsingEnabled = safeBrowsingDeferred.await().enabled,
                    parentalEnabled = parentalDeferred.await().enabled,
                    safeSearch = safeSearchDeferred.await(),
                    rewrites = rewritesDeferred.await(),
                    rewritesEnabled = rewriteSettingsDeferred.await(),
                )
            }
        }
    }

    override suspend fun setSafeBrowsingEnabled(enabled: Boolean): Result<Boolean> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            if (enabled) api.enableSafeBrowsing() else api.disableSafeBrowsing()
            api.getSafeBrowsingStatus().enabled
        }
    }

    override suspend fun setParentalEnabled(enabled: Boolean): Result<Boolean> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            if (enabled) api.enableParental() else api.disableParental()
            api.getParentalStatus().enabled
        }
    }

    override suspend fun updateSafeSearch(config: SafeSearchConfigDto): Result<SafeSearchConfigDto> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.updateSafeSearchSettings(config)
            api.getSafeSearchStatus()
        }
    }

    override suspend fun setRewritesEnabled(enabled: Boolean): Result<Boolean> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.updateRewriteSettings(EnabledStatusDto(enabled = enabled))
            api.getRewriteSettings().enabled
        }
    }

    override suspend fun addRewrite(
        domain: String,
        answer: String,
        enabled: Boolean,
    ): Result<List<RewriteEntryDto>> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        val trimmedDomain = domain.trim()
        val trimmedAnswer = answer.trim()
        if (trimmedDomain.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a domain or wildcard (e.g., home.lan or *.home.lan)."))
        }
        if (trimmedAnswer.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter an IP address, target domain, A, or AAAA."))
        }
        return executeApiCall {
            api.addRewrite(
                RewriteEntryDto(
                    domain = trimmedDomain,
                    answer = trimmedAnswer,
                    enabled = enabled,
                ),
            )
            api.getRewriteList()
        }
    }

    override suspend fun updateRewrite(
        target: RewriteEntryDto,
        updated: RewriteEntryDto,
    ): Result<List<RewriteEntryDto>> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        val trimmedDomain = updated.domain.trim()
        val trimmedAnswer = updated.answer.trim()
        if (trimmedDomain.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a domain or wildcard (e.g., home.lan or *.home.lan)."))
        }
        if (trimmedAnswer.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter an IP address, target domain, A, or AAAA."))
        }
        return executeApiCall {
            api.updateRewrite(
                RewriteUpdateRequest(
                    target = target,
                    update = RewriteEntryDto(
                        domain = trimmedDomain,
                        answer = trimmedAnswer,
                        enabled = updated.enabled,
                    ),
                ),
            )
            api.getRewriteList()
        }
    }

    override suspend fun deleteRewrite(entry: RewriteEntryDto): Result<List<RewriteEntryDto>> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.deleteRewrite(
                RewriteDeleteRequest(
                    domain = entry.domain,
                    answer = entry.answer,
                ),
            )
            api.getRewriteList()
        }
    }

    override suspend fun clearQueryLog(): Result<Unit> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.clearQueryLog()
        }
    }

    override suspend fun resetStats(): Result<Unit> {
        val api = activeApi ?: return Result.failure(IllegalStateException("Not connected to an AdGuard Home server."))
        return executeApiCall {
            api.resetStats()
        }
    }

    override fun disconnect() {
        activeConfig = null
        activeApi = null
        lastServerVersion = ""
        storage.clear()
    }

    private suspend fun <T> executeApiCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AdGuardApiException(mapErrorMessage(e), e))
        }
    }

    private suspend fun fetchSnapshot(
        api: AdGuardHomeApi,
        recentMillis: Long? = null,
    ): Result<DashboardSnapshot> {
        return try {
            val snapshot = coroutineScope {
                val statusDeferred = async { api.getStatus() }
                val statsDeferred = async { api.getStats(recentMillis = recentMillis) }
                DashboardSnapshot(
                    status = statusDeferred.await(),
                    stats = statsDeferred.await(),
                )
            }
            Result.success(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AdGuardApiException(mapErrorMessage(e), e))
        }
    }

    private fun mapErrorMessage(error: Throwable): String = when (error) {
        is HttpException -> {
            val bodyMessage = runCatching {
                error.response()?.errorBody()?.string()?.trim()?.take(200)
            }.getOrNull()?.takeIf { it.isNotBlank() }

            when (error.code()) {
                400, 422 -> bodyMessage ?: "Invalid request rejected by AdGuard Home (HTTP ${error.code()})."
                401 -> "Authentication failed. Check your username and password."
                403 -> "Access denied by AdGuard Home."
                404 -> "AdGuard Home API not found at this URL."
                in 500..599 -> bodyMessage ?: "AdGuard Home server error (HTTP ${error.code()})."
                else -> bodyMessage ?: "Request failed with HTTP ${error.code()}."
            }
        }
        is UnknownHostException -> "Unable to resolve server host. Check the URL and network."
        is ConnectException -> "Connection refused. Verify the server IP, port, and firewall."
        is SocketTimeoutException -> "Connection timed out. Is AdGuard Home reachable on this network? On Android 17+, ensure Local Network permission is allowed."
        is SSLException -> "TLS/SSL handshake failed. If your server uses a self-signed certificate, enable \"Trust self-signed HTTPS certificates\"."
        is IOException -> {
            if (error.message?.contains("CLEARTEXT", ignoreCase = true) == true) {
                "Plain HTTP is blocked by Android network security policy. Use HTTPS or configure local HTTP access."
            } else {
                error.message ?: "Network error while contacting AdGuard Home."
            }
        }
        else -> error.message ?: "Unexpected error communicating with AdGuard Home."
    }

    internal companion object {
        /**
         * AdGuard Home introduced the `reason` query param for `GET /control/querylog` in
         * v0.107.77 and removed `response_status` in v1.0.0.
         */
        fun supportsReasonQueryParam(version: String): Boolean {
            val match = Regex("""(\d+)\.(\d+)\.(\d+)""").find(version) ?: return true
            val major = match.groupValues[1].toIntOrNull() ?: return true
            val minor = match.groupValues[2].toIntOrNull() ?: return true
            val patch = match.groupValues[3].toIntOrNull() ?: return true
            return when {
                major > 0 -> true
                minor > 107 -> true
                minor == 107 -> patch >= 77
                else -> false
            }
        }
    }
}

class AdGuardApiException(
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
