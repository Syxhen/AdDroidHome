package com.syxhen.addroidhome.ui

import com.syxhen.addroidhome.data.model.DashboardSnapshot
import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.model.BlockedServiceDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.DnsQuestionDto
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.QueryLogResponseDto
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.SafeSearchEngine
import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.ServiceGroupDto
import com.syxhen.addroidhome.data.network.model.StatsDto
import com.syxhen.addroidhome.data.repository.AdGuardRepository
import com.syxhen.addroidhome.data.repository.BlockedServicesSnapshot
import com.syxhen.addroidhome.data.repository.FilterRefreshResult
import com.syxhen.addroidhome.data.repository.SettingsSnapshot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdGuardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `connect succeeds and switches to dashboard`() = runTest {
        val repository = FakeAdGuardRepository()
        val viewModel = AdGuardViewModel(repository)

        viewModel.onServerUrlChange("https://adguard.home.local")
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("secret")
        viewModel.connect()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppScreen.Dashboard, state.screen)
        assertEquals("adguard.home.local", state.dashboard.serverHost)
        assertEquals(500L, state.dashboard.snapshot?.stats?.numDnsQueries)
        assertNull(state.connection.errorMessage)
    }

    @Test
    fun `connect failure keeps user on connection screen with error message`() = runTest {
        val repository = FakeAdGuardRepository(connectFailure = IllegalStateException("Auth failed"))
        val viewModel = AdGuardViewModel(repository)

        viewModel.onServerUrlChange("https://adguard.home.local")
        viewModel.connect()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppScreen.Connection, state.screen)
        assertEquals("Auth failed", state.connection.errorMessage)
        assertFalse(state.connection.isConnecting)
    }

    @Test
    fun `saved config automatically restores dashboard session`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )

        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        assertEquals(AppScreen.Dashboard, viewModel.uiState.value.screen)
        assertEquals("adguard.home.local", viewModel.uiState.value.dashboard.serverHost)
    }

    @Test
    fun `pausing protection updates snapshot and disconnect returns to connection screen`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.setProtection(enabled = false, durationMillis = 300_000L)
        advanceUntilIdle()

        val pausedStatus = viewModel.uiState.value.dashboard.snapshot?.status
        assertFalse(pausedStatus?.protectionEnabled ?: true)
        assertEquals(300_000L, pausedStatus?.protectionDisabledDurationMillis)

        viewModel.disconnect()

        val afterDisconnect = viewModel.uiState.value
        assertEquals(AppScreen.Connection, afterDisconnect.screen)
        assertEquals("", afterDisconnect.connection.password)
        assertTrue(repository.disconnected)
    }

    @Test
    fun `selecting query log tab loads entries and supports filtering, search, and quick block`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.onSelectTab(MainTab.QueryLog)
        advanceUntilIdle()

        assertEquals(MainTab.QueryLog, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.queryLog.items.size)
        assertTrue(viewModel.uiState.value.queryLog.hasLoadedOnce)

        viewModel.onQueryLogFilterSelected(QueryLogFilter.Blocked)
        advanceUntilIdle()
        assertEquals(QueryLogFilter.Blocked, repository.lastQueryLogFilter)

        viewModel.onQueryLogSearchChange("tracker")
        advanceUntilIdle()
        assertEquals("tracker", repository.lastQueryLogSearch)

        val entry = viewModel.uiState.value.queryLog.items.first()
        viewModel.setDomainBlocked(entry, block = true)
        advanceUntilIdle()

        assertEquals("ads.tracker.net", repository.lastBlockedDomain)
        assertEquals(true, repository.lastBlockAction)
        assertEquals("Blocked ads.tracker.net", viewModel.uiState.value.queryLog.statusMessage)
    }

    @Test
    fun `selecting filters tab loads lists and supports toggle, refresh, and custom rules editing`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.onSelectTab(MainTab.Filters)
        advanceUntilIdle()

        val initialFilters = viewModel.uiState.value.filters
        assertEquals(MainTab.Filters, viewModel.uiState.value.selectedTab)
        assertTrue(initialFilters.hasLoadedOnce)
        assertEquals(1, initialFilters.status?.filters?.size)
        assertEquals("||ads.initial.test^", initialFilters.customRulesDraft)
        assertFalse(initialFilters.hasUnsavedCustomRules)

        val firstList = initialFilters.status!!.filters.first()
        viewModel.toggleFilterList(firstList, whitelist = false, enabled = false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.filters.status!!.filters.first().enabled)
        assertEquals("Disabled AdGuard DNS filter", viewModel.uiState.value.filters.statusMessage)

        viewModel.updateFilterListsFromUpstream(whitelist = false)
        advanceUntilIdle()
        assertEquals("Updated 1 blocklist", viewModel.uiState.value.filters.statusMessage)

        viewModel.onCustomRulesDraftChange("||ads.initial.test^\n@@||good.test^\n")
        assertTrue(viewModel.uiState.value.filters.hasUnsavedCustomRules)

        viewModel.discardCustomRulesChanges()
        assertFalse(viewModel.uiState.value.filters.hasUnsavedCustomRules)

        viewModel.onCustomRulesDraftChange("||ads.initial.test^\n||new.tracker.test^")
        viewModel.saveCustomRules()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.filters.hasUnsavedCustomRules)
        assertEquals(
            listOf("||ads.initial.test^", "||new.tracker.test^"),
            viewModel.uiState.value.filters.status?.userRules,
        )
        assertEquals("Saved 2 custom rule lines", viewModel.uiState.value.filters.statusMessage)
    }

    @Test
    fun `selecting services tab supports search, category filtering, toggling, and schedule-preserving save`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.onSelectTab(MainTab.Services)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value.blockedServices
        assertEquals(MainTab.Services, viewModel.uiState.value.selectedTab)
        assertTrue(loaded.hasLoadedOnce)
        assertEquals(setOf("tiktok"), loaded.savedIds)
        assertEquals(3, loaded.services.size)
        assertFalse(loaded.hasUnsavedChanges)

        viewModel.onBlockedServicesSearchChange("you")
        assertEquals(listOf("youtube"), viewModel.uiState.value.blockedServices.filteredServices.map { it.id })
        viewModel.onBlockedServicesSearchChange("")

        viewModel.onBlockedServicesGroupSelected("social_network")
        assertEquals(listOf("instagram", "tiktok"), viewModel.uiState.value.blockedServices.filteredServices.map { it.id })
        viewModel.onBlockedServicesGroupSelected(null)

        viewModel.onToggleBlockedService("youtube", blocked = true)
        assertTrue(viewModel.uiState.value.blockedServices.hasUnsavedChanges)
        assertEquals(1, viewModel.uiState.value.blockedServices.pendingChangeCount)

        viewModel.discardBlockedServicesChanges()
        assertFalse(viewModel.uiState.value.blockedServices.hasUnsavedChanges)

        viewModel.onToggleBlockedService("youtube", blocked = true)
        viewModel.saveBlockedServices()
        advanceUntilIdle()

        val savedState = viewModel.uiState.value.blockedServices
        assertFalse(savedState.hasUnsavedChanges)
        assertEquals(setOf("tiktok", "youtube"), savedState.savedIds)
        assertTrue(savedState.currentSchedule?.hasActiveSchedule == true)
        assertEquals("Updated blocked services (2 services blocked)", savedState.statusMessage)
    }

    @Test
    fun `openQueryLog switches tab and loads filtered results immediately`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.openQueryLog(search = " telemetry.example.com ", filter = QueryLogFilter.Blocked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MainTab.QueryLog, state.selectedTab)
        assertEquals("telemetry.example.com", state.queryLog.searchQuery)
        assertEquals(QueryLogFilter.Blocked, state.queryLog.selectedFilter)
        assertEquals("telemetry.example.com", repository.lastQueryLogSearch)
        assertEquals(QueryLogFilter.Blocked, repository.lastQueryLogFilter)
        assertTrue(state.queryLog.hasLoadedOnce)
    }

    @Test
    fun `selecting stats time range reloads with recentMillis and rolls back on failure`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
            defaultHourlyBuckets = 24,
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        assertEquals(StatsTimeRange.Default, viewModel.uiState.value.dashboard.selectedTimeRange)
        assertFalse(StatsTimeRange.Last7Days in viewModel.uiState.value.dashboard.availableTimeRanges)
        assertTrue(StatsTimeRange.Last24Hours in viewModel.uiState.value.dashboard.availableTimeRanges)

        viewModel.onSelectStatsTimeRange(StatsTimeRange.Last6Hours)
        advanceUntilIdle()

        assertEquals(StatsTimeRange.Last6Hours, viewModel.uiState.value.dashboard.selectedTimeRange)
        assertEquals(6 * 3_600_000L, repository.lastStatsRecentMillis)

        repository.refreshFailure = IllegalArgumentException("Invalid value of parameter recent")
        viewModel.onSelectStatsTimeRange(StatsTimeRange.Last12Hours)
        advanceUntilIdle()

        assertEquals(StatsTimeRange.Last6Hours, viewModel.uiState.value.dashboard.selectedTimeRange)
        assertEquals(
            "Invalid value of parameter recent",
            viewModel.uiState.value.dashboard.errorMessage,
        )
    }

    @Test
    fun `settings tab loads protection toggles, auto-enables safe search engines, manages rewrites, and clears data`() = runTest {
        val repository = FakeAdGuardRepository(
            savedConfig = ServerConfig("https://adguard.home.local", "admin", "pw"),
        )
        val viewModel = AdGuardViewModel(repository)
        advanceUntilIdle()

        viewModel.onSelectTab(MainTab.QueryLog)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.queryLog.items.size)

        viewModel.onSelectTab(MainTab.Settings)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.settings.hasLoadedOnce)
        assertEquals(1, viewModel.uiState.value.settings.rewrites.size)

        viewModel.setSafeBrowsingEnabled(true)
        viewModel.setParentalEnabled(true)
        viewModel.setSafeSearchEnabled(true)
        advanceUntilIdle()

        val afterToggles = viewModel.uiState.value.settings
        assertTrue(afterToggles.safeBrowsingEnabled)
        assertTrue(afterToggles.parentalEnabled)
        assertTrue(afterToggles.safeSearch.enabled)
        assertEquals(SafeSearchEngine.entries.size, afterToggles.safeSearch.enabledEngineCount)

        viewModel.toggleSafeSearchEngine(SafeSearchEngine.YouTube, false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.safeSearch.youTube)

        viewModel.addRewrite(domain = "plex.home.lan", answer = "192.168.1.90", enabled = true)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.settings.rewrites.size)

        val added = viewModel.uiState.value.settings.rewrites.last()
        viewModel.updateRewrite(added, added.copy(enabled = false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.rewrites.last().enabled)

        viewModel.deleteRewrite(viewModel.uiState.value.settings.rewrites.last())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.settings.rewrites.size)

        viewModel.clearQueryLog()
        advanceUntilIdle()
        assertTrue(repository.queryLogCleared)
        assertTrue(viewModel.uiState.value.queryLog.items.isEmpty())
        assertFalse(viewModel.uiState.value.queryLog.hasLoadedOnce)

        val refreshCountBeforeReset = repository.dashboardRefreshCount
        viewModel.resetStats()
        advanceUntilIdle()
        assertTrue(repository.statsReset)
        assertEquals(refreshCountBeforeReset + 1, repository.dashboardRefreshCount)
    }
}

private class FakeAdGuardRepository(
    private var savedConfig: ServerConfig? = null,
    private val connectFailure: Throwable? = null,
    defaultHourlyBuckets: Int = 0,
) : AdGuardRepository {

    var disconnected = false
    var lastStatsRecentMillis: Long? = null
    var dashboardRefreshCount: Int = 0
    var refreshFailure: Throwable? = null
    var lastQueryLogSearch: String? = null
    var lastQueryLogFilter: QueryLogFilter = QueryLogFilter.All
    var lastBlockedDomain: String? = null
    var lastBlockAction: Boolean? = null
    var queryLogCleared: Boolean = false
    var statsReset: Boolean = false

    private var settingsSnapshot = SettingsSnapshot(
        safeBrowsingEnabled = false,
        parentalEnabled = false,
        safeSearch = SafeSearchConfigDto(),
        rewrites = listOf(
            RewriteEntryDto(domain = "nas.home.lan", answer = "192.168.1.50", enabled = true),
        ),
        rewritesEnabled = true,
    )

    private var snapshot = DashboardSnapshot(
        status = ServerStatusDto(
            protectionEnabled = true,
            version = "v0.107.80",
            running = true,
        ),
        stats = StatsDto(
            timeUnits = "hours",
            numDnsQueries = 500L,
            numBlockedFiltering = 125L,
            dnsQueries = List(defaultHourlyBuckets) { 20L },
            blockedFiltering = List(defaultHourlyBuckets) { 5L },
        ),
    )

    private var filterStatus = FilterStatusDto(
        enabled = true,
        intervalHours = 24,
        filters = listOf(
            FilterListDto(
                id = 1L,
                enabled = true,
                url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
                name = "AdGuard DNS filter",
                rulesCount = 58_000L,
            ),
        ),
        whitelistFilters = emptyList(),
        userRules = listOf("||ads.initial.test^"),
    )

    private var blockedServicesSchedule = BlockedServicesScheduleDto(
        ids = listOf("tiktok"),
        schedule = JsonObject(
            mapOf(
                "time_zone" to JsonPrimitive("Europe/Rome"),
                "mon" to JsonObject(mapOf("start" to JsonPrimitive(0), "end" to JsonPrimitive(3_600_000))),
            ),
        ),
    )

    private val catalogServices = listOf(
        BlockedServiceDto(id = "instagram", name = "Instagram", groupId = "social_network"),
        BlockedServiceDto(id = "tiktok", name = "TikTok", groupId = "social_network"),
        BlockedServiceDto(id = "youtube", name = "YouTube", groupId = "streaming"),
    )

    private val catalogGroups = listOf(
        ServiceGroupDto("social_network"),
        ServiceGroupDto("streaming"),
    )

    override fun getSavedConfig(): ServerConfig? = savedConfig

    override suspend fun connectAndLoadDashboard(config: ServerConfig): Result<DashboardSnapshot> {
        connectFailure?.let { return Result.failure(it) }
        savedConfig = config
        return Result.success(snapshot)
    }

    override suspend fun refreshDashboard(recentMillis: Long?): Result<DashboardSnapshot> {
        dashboardRefreshCount++
        lastStatsRecentMillis = recentMillis
        refreshFailure?.let { return Result.failure(it) }
        return Result.success(snapshot)
    }

    override suspend fun setProtection(
        enabled: Boolean,
        durationMillis: Long?,
        recentMillis: Long?,
    ): Result<DashboardSnapshot> {
        lastStatsRecentMillis = recentMillis
        snapshot = snapshot.copy(
            status = snapshot.status.copy(
                protectionEnabled = enabled,
                protectionDisabledDurationMillis = if (enabled) 0L else (durationMillis ?: 0L),
            ),
        )
        return Result.success(snapshot)
    }

    override suspend fun getQueryLog(
        search: String?,
        filter: QueryLogFilter,
        olderThan: String?,
        limit: Int,
    ): Result<QueryLogResponseDto> {
        lastQueryLogSearch = search
        lastQueryLogFilter = filter
        return Result.success(
            QueryLogResponseDto(
                oldest = "2026-11-26T01:00:00+01:00",
                data = listOf(
                    QueryLogItemDto(
                        time = "2026-11-26T01:05:00+01:00",
                        question = DnsQuestionDto(name = "ads.tracker.net", type = "A"),
                        client = "192.168.1.15",
                        reason = "NotFilteredNotFound",
                    ),
                ),
            ),
        )
    }

    override suspend fun setDomainBlocked(
        domain: String,
        block: Boolean,
        blockedByCustomRuleOnly: Boolean,
    ): Result<Unit> {
        lastBlockedDomain = domain
        lastBlockAction = block
        return Result.success(Unit)
    }

    override suspend fun getFilteringStatus(): Result<FilterStatusDto> = Result.success(filterStatus)

    override suspend fun setFilteringEnabled(
        enabled: Boolean,
        intervalHours: Int,
    ): Result<FilterStatusDto> {
        filterStatus = filterStatus.copy(enabled = enabled, intervalHours = intervalHours)
        return Result.success(filterStatus)
    }

    override suspend fun setFilterListEnabled(
        filter: FilterListDto,
        whitelist: Boolean,
        enabled: Boolean,
    ): Result<FilterStatusDto> {
        filterStatus = if (whitelist) {
            filterStatus.copy(
                whitelistFilters = filterStatus.whitelistFilters.map {
                    if (it.url == filter.url) it.copy(enabled = enabled) else it
                },
            )
        } else {
            filterStatus.copy(
                filters = filterStatus.filters.map {
                    if (it.url == filter.url) it.copy(enabled = enabled) else it
                },
            )
        }
        return Result.success(filterStatus)
    }

    override suspend fun addFilterList(
        name: String,
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto> {
        val added = FilterListDto(id = 99L, enabled = true, url = url, name = name, rulesCount = 100L)
        filterStatus = if (whitelist) {
            filterStatus.copy(whitelistFilters = filterStatus.whitelistFilters + added)
        } else {
            filterStatus.copy(filters = filterStatus.filters + added)
        }
        return Result.success(filterStatus)
    }

    override suspend fun removeFilterList(
        url: String,
        whitelist: Boolean,
    ): Result<FilterStatusDto> {
        filterStatus = if (whitelist) {
            filterStatus.copy(whitelistFilters = filterStatus.whitelistFilters.filterNot { it.url == url })
        } else {
            filterStatus.copy(filters = filterStatus.filters.filterNot { it.url == url })
        }
        return Result.success(filterStatus)
    }

    override suspend fun refreshFilterLists(whitelist: Boolean): Result<FilterRefreshResult> {
        val updatedCount = if (whitelist) filterStatus.whitelistFilters.size else filterStatus.filters.size
        return Result.success(FilterRefreshResult(updatedCount = updatedCount, status = filterStatus))
    }

    override suspend fun setCustomRules(rules: List<String>): Result<FilterStatusDto> {
        filterStatus = filterStatus.copy(userRules = rules)
        return Result.success(filterStatus)
    }

    override suspend fun getBlockedServicesSnapshot(): Result<BlockedServicesSnapshot> =
        Result.success(
            BlockedServicesSnapshot(
                services = catalogServices,
                groups = catalogGroups,
                current = blockedServicesSchedule,
            ),
        )

    override suspend fun updateBlockedServices(
        blockedIds: Set<String>,
    ): Result<BlockedServicesScheduleDto> {
        blockedServicesSchedule = blockedServicesSchedule.copy(ids = blockedIds.sorted())
        return Result.success(blockedServicesSchedule)
    }

    override suspend fun getSettingsSnapshot(): Result<SettingsSnapshot> =
        Result.success(settingsSnapshot)

    override suspend fun setSafeBrowsingEnabled(enabled: Boolean): Result<Boolean> {
        settingsSnapshot = settingsSnapshot.copy(safeBrowsingEnabled = enabled)
        return Result.success(enabled)
    }

    override suspend fun setParentalEnabled(enabled: Boolean): Result<Boolean> {
        settingsSnapshot = settingsSnapshot.copy(parentalEnabled = enabled)
        return Result.success(enabled)
    }

    override suspend fun updateSafeSearch(config: SafeSearchConfigDto): Result<SafeSearchConfigDto> {
        settingsSnapshot = settingsSnapshot.copy(safeSearch = config)
        return Result.success(config)
    }

    override suspend fun setRewritesEnabled(enabled: Boolean): Result<Boolean> {
        settingsSnapshot = settingsSnapshot.copy(rewritesEnabled = enabled)
        return Result.success(enabled)
    }

    override suspend fun addRewrite(
        domain: String,
        answer: String,
        enabled: Boolean,
    ): Result<List<RewriteEntryDto>> {
        val updated = settingsSnapshot.rewrites + RewriteEntryDto(domain.trim(), answer.trim(), enabled)
        settingsSnapshot = settingsSnapshot.copy(rewrites = updated)
        return Result.success(updated)
    }

    override suspend fun updateRewrite(
        target: RewriteEntryDto,
        updated: RewriteEntryDto,
    ): Result<List<RewriteEntryDto>> {
        val next = settingsSnapshot.rewrites.map {
            if (it.domain == target.domain && it.answer == target.answer) updated else it
        }
        settingsSnapshot = settingsSnapshot.copy(rewrites = next)
        return Result.success(next)
    }

    override suspend fun deleteRewrite(entry: RewriteEntryDto): Result<List<RewriteEntryDto>> {
        val next = settingsSnapshot.rewrites.filterNot {
            it.domain == entry.domain && it.answer == entry.answer
        }
        settingsSnapshot = settingsSnapshot.copy(rewrites = next)
        return Result.success(next)
    }

    override suspend fun clearQueryLog(): Result<Unit> {
        queryLogCleared = true
        return Result.success(Unit)
    }

    override suspend fun resetStats(): Result<Unit> {
        statsReset = true
        return Result.success(Unit)
    }

    override fun disconnect() {
        disconnected = true
        savedConfig = null
    }
}
