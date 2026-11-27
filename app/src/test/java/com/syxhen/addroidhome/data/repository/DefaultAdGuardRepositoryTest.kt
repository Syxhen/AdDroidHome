package com.syxhen.addroidhome.data.repository

import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.api.AdGuardHomeApi
import com.syxhen.addroidhome.data.network.model.AddFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.BlockedServiceDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesAllDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.DnsQuestionDto
import com.syxhen.addroidhome.data.network.model.EnabledStatusDto
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.QueryLogResponseDto
import com.syxhen.addroidhome.data.network.model.RefreshFiltersRequest
import com.syxhen.addroidhome.data.network.model.RefreshFiltersResponse
import com.syxhen.addroidhome.data.network.model.RemoveFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.RewriteDeleteRequest
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.RewriteUpdateRequest
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.ServiceGroupDto
import com.syxhen.addroidhome.data.network.model.SetFilterUrlRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringConfigRequest
import com.syxhen.addroidhome.data.network.model.SetFilteringRulesRequest
import com.syxhen.addroidhome.data.network.model.SetProtectionRequest
import com.syxhen.addroidhome.data.network.model.StatsDto
import com.syxhen.addroidhome.data.storage.ServerConfigStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DefaultAdGuardRepositoryTest {

    @Test
    fun `connect normalizes url, fetches status and stats, and saves config`() = runTest {
        val storage = FakeServerConfigStorage()
        val fakeApi = FakeAdGuardHomeApi()
        var capturedConfig: ServerConfig? = null
        val repository = DefaultAdGuardRepository(storage) { config ->
            capturedConfig = config
            fakeApi
        }

        val result = repository.connectAndLoadDashboard(
            ServerConfig(
                baseUrl = "192.168.1.2:3000/control/",
                username = " admin ",
                password = "secret",
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.2:3000", capturedConfig?.baseUrl)
        assertEquals("admin", capturedConfig?.username)
        assertEquals("http://192.168.1.2:3000", storage.load()?.baseUrl)
        assertEquals("v0.107.80", result.getOrThrow().status.version)
        assertEquals(1200L, result.getOrThrow().stats.numDnsQueries)
    }

    @Test
    fun `connect fails without saving config when network fails`() = runTest {
        val storage = FakeServerConfigStorage()
        val fakeApi = FakeAdGuardHomeApi(failWith = IOException("Network unreachable"))
        val repository = DefaultAdGuardRepository(storage) { fakeApi }

        val result = repository.connectAndLoadDashboard(
            ServerConfig(baseUrl = "https://adguard.home.local"),
        )

        assertTrue(result.isFailure)
        assertNull(storage.load())
    }

    @Test
    fun `connect rejects plain http for public domain`() = runTest {
        val storage = FakeServerConfigStorage()
        val repository = DefaultAdGuardRepository(storage) { FakeAdGuardHomeApi() }

        val result = repository.connectAndLoadDashboard(
            ServerConfig(baseUrl = "http://adguard.example.com"),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("https://", ignoreCase = true) == true)
        assertNull(storage.load())
    }

    @Test
    fun `setProtection sends pause duration and reloads status`() = runTest {
        val storage = FakeServerConfigStorage()
        val fakeApi = FakeAdGuardHomeApi()
        val repository = DefaultAdGuardRepository(storage) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val result = repository.setProtection(enabled = false, durationMillis = 300_000L)

        assertTrue(result.isSuccess)
        assertEquals(
            SetProtectionRequest(enabled = false, durationMillis = 300_000L),
            fakeApi.lastProtectionRequest,
        )
        assertFalse(result.getOrThrow().status.protectionEnabled)
        assertEquals(300_000L, result.getOrThrow().status.protectionDisabledDurationMillis)
    }

    @Test
    fun `getQueryLog uses reason parameter on v0_107_77+ and legacy response_status on older servers`() = runTest {
        val modernApi = FakeAdGuardHomeApi(serverVersion = "v1.0.0")
        val modernRepo = DefaultAdGuardRepository(FakeServerConfigStorage()) { modernApi }
        modernRepo.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        modernRepo.getQueryLog(search = "ads", filter = QueryLogFilter.Blocked)

        assertEquals(QueryLogFilter.Blocked.reasons, modernApi.lastQueryLogReasons)
        assertNull(modernApi.lastQueryLogResponseStatus)
        assertEquals("ads", modernApi.lastQueryLogSearch)

        val legacyApi = FakeAdGuardHomeApi(serverVersion = "v0.107.72")
        val legacyRepo = DefaultAdGuardRepository(FakeServerConfigStorage()) { legacyApi }
        legacyRepo.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        legacyRepo.getQueryLog(search = "ads", filter = QueryLogFilter.Blocked)

        assertNull(legacyApi.lastQueryLogReasons)
        assertEquals("blocked", legacyApi.lastQueryLogResponseStatus)
    }

    @Test
    fun `setDomainBlocked fetches existing user rules and saves updated rule set`() = runTest {
        val fakeApi = FakeAdGuardHomeApi(initialUserRules = listOf("@@||ads.example.com^", "||keep.test^"))
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val result = repository.setDomainBlocked(domain = "ads.example.com", block = true)

        assertTrue(result.isSuccess)
        assertEquals(listOf("||keep.test^", "||ads.example.com^"), fakeApi.savedUserRules)
    }

    @Test
    fun `filter list toggle, add, remove, refresh, and custom rules update server state`() = runTest {
        val initialFilter = FilterListDto(
            id = 1L,
            enabled = true,
            url = "https://example.com/blocklist.txt",
            name = "Base Blocklist",
            rulesCount = 12_000L,
        )
        val fakeApi = FakeAdGuardHomeApi(initialFilters = listOf(initialFilter))
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val toggleResult = repository.setFilterListEnabled(
            filter = initialFilter,
            whitelist = false,
            enabled = false,
        )
        assertTrue(toggleResult.isSuccess)
        assertFalse(toggleResult.getOrThrow().filters.first().enabled)

        val addResult = repository.addFilterList(
            name = "Trusted Allowlist",
            url = "https://example.com/allow.txt",
            whitelist = true,
        )
        assertTrue(addResult.isSuccess)
        assertEquals(1, addResult.getOrThrow().whitelistFilters.size)

        val invalidAdd = repository.addFilterList(
            name = "Bad",
            url = "ftp://example.com/list.txt",
            whitelist = false,
        )
        assertTrue(invalidAdd.isFailure)

        val refreshResult = repository.refreshFilterLists(whitelist = false)
        assertTrue(refreshResult.isSuccess)
        assertEquals(1, refreshResult.getOrThrow().updatedCount)

        val rulesResult = repository.setCustomRules(listOf("||ads.example.net^", "@@||good.example.net^"))
        assertTrue(rulesResult.isSuccess)
        assertEquals(listOf("||ads.example.net^", "@@||good.example.net^"), rulesResult.getOrThrow().userRules)

        val removeResult = repository.removeFilterList(
            url = "https://example.com/allow.txt",
            whitelist = true,
        )
        assertTrue(removeResult.isSuccess)
        assertTrue(removeResult.getOrThrow().whitelistFilters.isEmpty())
    }

    @Test
    fun `updateBlockedServices updates ids while preserving server schedule verbatim`() = runTest {
        val serverSchedule = JsonObject(
            mapOf(
                "time_zone" to JsonPrimitive("Europe/Rome"),
                "mon" to JsonObject(mapOf("start" to JsonPrimitive(32_400_000), "end" to JsonPrimitive(64_800_000))),
            ),
        )
        val fakeApi = FakeAdGuardHomeApi(
            initialBlockedServicesSchedule = BlockedServicesScheduleDto(
                ids = listOf("tiktok"),
                schedule = serverSchedule,
            ),
        )
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val snapshot = repository.getBlockedServicesSnapshot().getOrThrow()
        assertEquals(listOf("Instagram", "TikTok", "YouTube"), snapshot.services.map { it.displayName })
        assertEquals(serverSchedule, snapshot.current.schedule)

        val updated = repository.updateBlockedServices(setOf("youtube", "instagram")).getOrThrow()
        assertEquals(listOf("instagram", "youtube"), updated.ids)
        assertEquals(serverSchedule, updated.schedule)
        assertEquals(serverSchedule, fakeApi.lastBlockedServicesUpdate?.schedule)
    }

    @Test
    fun `disconnect clears storage and active session`() = runTest {
        val storage = FakeServerConfigStorage()
        val repository = DefaultAdGuardRepository(storage) { FakeAdGuardHomeApi() }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        repository.disconnect()

        assertNull(storage.load())
        assertTrue(repository.refreshDashboard().isFailure)
    }

    @Test
    fun `refreshDashboard and setProtection forward recentMillis to getStats`() = runTest {
        val fakeApi = FakeAdGuardHomeApi()
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))
        assertNull(fakeApi.lastStatsRecentMillis)

        val sixHoursMillis = 6 * 3_600_000L
        val refreshResult = repository.refreshDashboard(recentMillis = sixHoursMillis)
        assertTrue(refreshResult.isSuccess)
        assertEquals(sixHoursMillis, fakeApi.lastStatsRecentMillis)

        val oneHourMillis = 3_600_000L
        val pauseResult = repository.setProtection(
            enabled = false,
            durationMillis = 300_000L,
            recentMillis = oneHourMillis,
        )
        assertTrue(pauseResult.isSuccess)
        assertEquals(oneHourMillis, fakeApi.lastStatsRecentMillis)
    }

    @Test
    fun `getQueryLog sends SafeAndParental reasons on modern servers`() = runTest {
        val fakeApi = FakeAdGuardHomeApi(serverVersion = "v0.107.80")
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val result = repository.getQueryLog(filter = QueryLogFilter.SafeAndParental)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("FilteredSafeBrowsing", "FilteredParental", "FilteredSafeSearch"),
            fakeApi.lastQueryLogReasons,
        )
        assertNull(fakeApi.lastQueryLogResponseStatus)
    }

    @Test
    fun `getSettingsSnapshot handles 404 rewrite settings gracefully and updates toggles and rewrites`() = runTest {
        val fakeApi = FakeAdGuardHomeApi(
            rewriteSettingsNotFound = true,
            initialRewrites = listOf(
                RewriteEntryDto(domain = "nas.lan", answer = "192.168.1.50", enabled = true),
            ),
        )
        val repository = DefaultAdGuardRepository(FakeServerConfigStorage()) { fakeApi }
        repository.connectAndLoadDashboard(ServerConfig(baseUrl = "https://adguard.home.local"))

        val snapshot = repository.getSettingsSnapshot().getOrThrow()
        assertFalse(snapshot.safeBrowsingEnabled)
        assertFalse(snapshot.parentalEnabled)
        assertNull(snapshot.rewritesEnabled)
        assertEquals(1, snapshot.rewrites.size)

        assertTrue(repository.setSafeBrowsingEnabled(true).getOrThrow())
        assertTrue(repository.setParentalEnabled(true).getOrThrow())
        val updatedSafeSearch = repository.updateSafeSearch(
            SafeSearchConfigDto(enabled = true, google = true, youTube = true),
        ).getOrThrow()
        assertTrue(updatedSafeSearch.enabled)
        assertTrue(updatedSafeSearch.google)

        assertTrue(repository.addRewrite("", "192.168.1.1").isFailure)
        val afterAdd = repository.addRewrite(" home.lan ", " 192.168.1.2 ", enabled = true).getOrThrow()
        assertEquals(2, afterAdd.size)
        assertEquals("home.lan", afterAdd.last().domain)

        val target = afterAdd.last()
        val afterUpdate = repository.updateRewrite(
            target = target,
            updated = target.copy(answer = "192.168.1.3", enabled = false),
        ).getOrThrow()
        assertEquals("192.168.1.3", afterUpdate.last().answer)
        assertFalse(afterUpdate.last().enabled)

        val afterDelete = repository.deleteRewrite(afterUpdate.last()).getOrThrow()
        assertEquals(1, afterDelete.size)

        assertTrue(repository.clearQueryLog().isSuccess)
        assertTrue(repository.resetStats().isSuccess)
        assertTrue(fakeApi.queryLogCleared)
        assertTrue(fakeApi.statsReset)
    }
}

private class FakeServerConfigStorage(
    private var saved: ServerConfig? = null,
) : ServerConfigStorage {
    override fun load(): ServerConfig? = saved
    override fun save(config: ServerConfig) {
        saved = config
    }

    override fun clear() {
        saved = null
    }
}

private class FakeAdGuardHomeApi(
    serverVersion: String = "v0.107.80",
    initialUserRules: List<String> = emptyList(),
    initialFilters: List<FilterListDto> = emptyList(),
    initialWhitelistFilters: List<FilterListDto> = emptyList(),
    initialBlockedServicesSchedule: BlockedServicesScheduleDto = BlockedServicesScheduleDto(),
    initialRewrites: List<RewriteEntryDto> = emptyList(),
    private val rewriteSettingsNotFound: Boolean = false,
    private val failWith: Throwable? = null,
) : AdGuardHomeApi {

    private var status = ServerStatusDto(
        dnsAddresses = listOf("192.168.1.2"),
        dnsPort = 53,
        httpPort = 3000,
        protectionEnabled = true,
        protectionDisabledDurationMillis = 0L,
        running = true,
        version = serverVersion,
    )

    private val stats = StatsDto(
        numDnsQueries = 1200L,
        numBlockedFiltering = 300L,
    )

    private var filteringEnabled: Boolean = true
    private var filteringIntervalHours: Int = 24
    private var filters: MutableList<FilterListDto> = initialFilters.toMutableList()
    private var whitelistFilters: MutableList<FilterListDto> = initialWhitelistFilters.toMutableList()
    private var nextFilterId: Long = 100L

    var lastProtectionRequest: SetProtectionRequest? = null
    var lastStatsRecentMillis: Long? = null
    var lastQueryLogSearch: String? = null
    var lastQueryLogReasons: List<String>? = null
    var lastQueryLogResponseStatus: String? = null
    var savedUserRules: List<String> = initialUserRules
    private var blockedServicesSchedule: BlockedServicesScheduleDto = initialBlockedServicesSchedule
    var lastBlockedServicesUpdate: BlockedServicesScheduleDto? = null

    override suspend fun getStatus(): ServerStatusDto {
        failWith?.let { throw it }
        return status
    }

    override suspend fun getStats(recentMillis: Long?): StatsDto {
        failWith?.let { throw it }
        lastStatsRecentMillis = recentMillis
        return stats
    }

    override suspend fun setProtection(request: SetProtectionRequest) {
        failWith?.let { throw it }
        lastProtectionRequest = request
        status = status.copy(
            protectionEnabled = request.enabled,
            protectionDisabledDurationMillis = if (request.enabled) 0L else (request.durationMillis ?: 0L),
        )
    }

    override suspend fun getQueryLog(
        search: String?,
        olderThan: String?,
        limit: Int?,
        reasons: List<String>?,
        responseStatus: String?,
    ): QueryLogResponseDto {
        failWith?.let { throw it }
        lastQueryLogSearch = search
        lastQueryLogReasons = reasons
        lastQueryLogResponseStatus = responseStatus
        return QueryLogResponseDto(
            oldest = "2026-11-26T00:00:00+01:00",
            data = listOf(
                QueryLogItemDto(
                    time = "2026-11-26T00:00:00+01:00",
                    question = DnsQuestionDto(name = "ads.example.com", type = "A"),
                    client = "192.168.1.10",
                    reason = "FilteredBlackList",
                ),
            ),
        )
    }

    override suspend fun getFilteringStatus(): FilterStatusDto {
        failWith?.let { throw it }
        return FilterStatusDto(
            enabled = filteringEnabled,
            intervalHours = filteringIntervalHours,
            filters = filters.toList(),
            whitelistFilters = whitelistFilters.toList(),
            userRules = savedUserRules,
        )
    }

    override suspend fun setFilteringConfig(request: SetFilteringConfigRequest) {
        failWith?.let { throw it }
        filteringEnabled = request.enabled
        filteringIntervalHours = request.intervalHours
    }

    override suspend fun setFilterUrl(request: SetFilterUrlRequest) {
        failWith?.let { throw it }
        val target = if (request.whitelist) whitelistFilters else filters
        val idx = target.indexOfFirst { it.url == request.url }
        if (idx >= 0) {
            target[idx] = target[idx].copy(
                name = request.data.name,
                url = request.data.url,
                enabled = request.data.enabled,
            )
        }
    }

    override suspend fun addFilterUrl(request: AddFilterUrlRequest): ResponseBody {
        failWith?.let { throw it }
        val target = if (request.whitelist) whitelistFilters else filters
        target.add(
            FilterListDto(
                id = nextFilterId++,
                enabled = true,
                url = request.url,
                name = request.name,
                rulesCount = 500L,
            ),
        )
        return "OK 500 rules\n".toResponseBody("text/plain".toMediaType())
    }

    override suspend fun removeFilterUrl(request: RemoveFilterUrlRequest): ResponseBody {
        failWith?.let { throw it }
        val target = if (request.whitelist) whitelistFilters else filters
        target.removeAll { it.url == request.url }
        return "OK 500 rules\n".toResponseBody("text/plain".toMediaType())
    }

    override suspend fun refreshFilters(request: RefreshFiltersRequest): RefreshFiltersResponse {
        failWith?.let { throw it }
        val count = if (request.whitelist) whitelistFilters.size else filters.size
        return RefreshFiltersResponse(updated = count)
    }

    override suspend fun setFilteringRules(request: SetFilteringRulesRequest) {
        failWith?.let { throw it }
        savedUserRules = request.rules
    }

    override suspend fun getAllBlockedServices(): BlockedServicesAllDto {
        failWith?.let { throw it }
        return BlockedServicesAllDto(
            blockedServices = listOf(
                BlockedServiceDto(id = "youtube", name = "YouTube", groupId = "streaming", rules = listOf("||youtube.com^")),
                BlockedServiceDto(id = "tiktok", name = "TikTok", groupId = "social_network", rules = listOf("||tiktok.com^")),
                BlockedServiceDto(id = "instagram", name = "Instagram", groupId = "social_network", rules = listOf("||instagram.com^")),
            ),
            groups = listOf(ServiceGroupDto("social_network"), ServiceGroupDto("streaming")),
        )
    }

    override suspend fun getBlockedServices(): BlockedServicesScheduleDto {
        failWith?.let { throw it }
        return blockedServicesSchedule
    }

    override suspend fun updateBlockedServices(request: BlockedServicesScheduleDto) {
        failWith?.let { throw it }
        lastBlockedServicesUpdate = request
        blockedServicesSchedule = request
    }

    private var safeBrowsingEnabled: Boolean = false
    private var parentalEnabled: Boolean = false
    private var safeSearchConfig: SafeSearchConfigDto = SafeSearchConfigDto()
    private var rewritesEnabled: Boolean = true
    private var rewrites: MutableList<RewriteEntryDto> = initialRewrites.toMutableList()
    var queryLogCleared: Boolean = false
    var statsReset: Boolean = false

    override suspend fun getSafeBrowsingStatus(): EnabledStatusDto {
        failWith?.let { throw it }
        return EnabledStatusDto(safeBrowsingEnabled)
    }

    override suspend fun enableSafeBrowsing() {
        failWith?.let { throw it }
        safeBrowsingEnabled = true
    }

    override suspend fun disableSafeBrowsing() {
        failWith?.let { throw it }
        safeBrowsingEnabled = false
    }

    override suspend fun getParentalStatus(): EnabledStatusDto {
        failWith?.let { throw it }
        return EnabledStatusDto(parentalEnabled)
    }

    override suspend fun enableParental() {
        failWith?.let { throw it }
        parentalEnabled = true
    }

    override suspend fun disableParental() {
        failWith?.let { throw it }
        parentalEnabled = false
    }

    override suspend fun getSafeSearchStatus(): SafeSearchConfigDto {
        failWith?.let { throw it }
        return safeSearchConfig
    }

    override suspend fun updateSafeSearchSettings(request: SafeSearchConfigDto) {
        failWith?.let { throw it }
        safeSearchConfig = request
    }

    override suspend fun getRewriteList(): List<RewriteEntryDto> {
        failWith?.let { throw it }
        return rewrites.toList()
    }

    override suspend fun getRewriteSettings(): EnabledStatusDto {
        failWith?.let { throw it }
        if (rewriteSettingsNotFound) {
            throw HttpException(
                Response.error<EnabledStatusDto>(
                    404,
                    "Not Found".toResponseBody("text/plain".toMediaType()),
                ),
            )
        }
        return EnabledStatusDto(rewritesEnabled)
    }

    override suspend fun updateRewriteSettings(request: EnabledStatusDto) {
        failWith?.let { throw it }
        rewritesEnabled = request.enabled
    }

    override suspend fun addRewrite(request: RewriteEntryDto) {
        failWith?.let { throw it }
        rewrites.add(request)
    }

    override suspend fun deleteRewrite(request: RewriteDeleteRequest) {
        failWith?.let { throw it }
        rewrites.removeAll { it.domain == request.domain && it.answer == request.answer }
    }

    override suspend fun updateRewrite(request: RewriteUpdateRequest) {
        failWith?.let { throw it }
        val index = rewrites.indexOfFirst {
            it.domain == request.target.domain && it.answer == request.target.answer
        }
        if (index >= 0) {
            rewrites[index] = request.update
        }
    }

    override suspend fun clearQueryLog() {
        failWith?.let { throw it }
        queryLogCleared = true
    }

    override suspend fun resetStats() {
        failWith?.let { throw it }
        statsReset = true
    }
}
