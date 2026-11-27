package com.syxhen.addroidhome.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.syxhen.addroidhome.data.network.model.DnsAnswerDto
import com.syxhen.addroidhome.data.network.model.DnsQuestionDto
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.QueryLogClientInfoDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.ResultRuleDto
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.SafeSearchEngine
import com.syxhen.addroidhome.ui.dashboard.DashboardContent
import com.syxhen.addroidhome.ui.dashboard.sampleDashboardSnapshot
import com.syxhen.addroidhome.ui.filters.FiltersContent
import com.syxhen.addroidhome.ui.icons.apps
import com.syxhen.addroidhome.ui.icons.dashboard
import com.syxhen.addroidhome.ui.icons.filter_list
import com.syxhen.addroidhome.ui.icons.logout
import com.syxhen.addroidhome.ui.icons.receipt_long
import com.syxhen.addroidhome.ui.icons.refresh
import com.syxhen.addroidhome.ui.icons.settings
import com.syxhen.addroidhome.ui.querylog.QueryLogContent
import com.syxhen.addroidhome.ui.querylog.sampleQueryLogItems
import com.syxhen.addroidhome.ui.services.BlockedServicesContent
import com.syxhen.addroidhome.ui.settings.SettingsContent
import com.syxhen.addroidhome.ui.settings.sampleSettingsState
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedHomeScreen(
    selectedTab: MainTab,
    dashboardState: DashboardUiState,
    queryLogState: QueryLogUiState,
    filtersState: FiltersUiState,
    blockedServicesState: BlockedServicesUiState,
    settingsState: SettingsUiState,
    onSelectTab: (MainTab) -> Unit,
    onRefreshDashboard: () -> Unit,
    onSelectStatsTimeRange: (StatsTimeRange) -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onPauseProtection: (Long) -> Unit,
    onOpenQueryLog: (search: String, filter: QueryLogFilter) -> Unit,
    onQueryLogSearchChange: (String) -> Unit,
    onQueryLogSearchSubmit: () -> Unit,
    onQueryLogFilterSelected: (QueryLogFilter) -> Unit,
    onRefreshQueryLog: () -> Unit,
    onLoadMoreQueryLog: () -> Unit,
    onSetDomainBlocked: (QueryLogItemDto, Boolean) -> Unit,
    onFiltersSectionSelected: (FiltersSection) -> Unit,
    onRefreshFiltersStatus: () -> Unit,
    onSetFilteringEnabled: (Boolean) -> Unit,
    onToggleFilterList: (FilterListDto, Boolean, Boolean) -> Unit,
    onAddFilterList: (name: String, url: String, whitelist: Boolean) -> Unit,
    onRemoveFilterList: (FilterListDto, Boolean) -> Unit,
    onCheckForFilterUpdates: (whitelist: Boolean) -> Unit,
    onCustomRulesDraftChange: (String) -> Unit,
    onDiscardCustomRules: () -> Unit,
    onSaveCustomRules: () -> Unit,
    onBlockedServicesSearchChange: (String) -> Unit,
    onBlockedServicesGroupSelected: (String?) -> Unit,
    onBlockedServicesShowBlockedOnlyChange: (Boolean) -> Unit,
    onToggleBlockedService: (String, Boolean) -> Unit,
    onRefreshBlockedServices: () -> Unit,
    onDiscardBlockedServicesChanges: () -> Unit,
    onSaveBlockedServices: () -> Unit,
    onRefreshSettings: () -> Unit,
    onSetSafeBrowsingEnabled: (Boolean) -> Unit,
    onSetParentalEnabled: (Boolean) -> Unit,
    onSetSafeSearchEnabled: (Boolean) -> Unit,
    onToggleSafeSearchEngine: (SafeSearchEngine, Boolean) -> Unit,
    onSetRewritesEnabled: (Boolean) -> Unit,
    onAddRewrite: (domain: String, answer: String, enabled: Boolean) -> Unit,
    onUpdateRewrite: (target: RewriteEntryDto, updated: RewriteEntryDto) -> Unit,
    onDeleteRewrite: (RewriteEntryDto) -> Unit,
    onClearQueryLog: () -> Unit,
    onResetStats: () -> Unit,
    onDisconnect: () -> Unit,
    onDismissDashboardError: () -> Unit,
    onDismissQueryLogFeedback: () -> Unit,
    onDismissFiltersFeedback: () -> Unit,
    onDismissBlockedServicesFeedback: () -> Unit,
    onDismissSettingsFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(dashboardState.errorMessage) {
        val message = dashboardState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissDashboardError()
    }

    val queryLogFeedback = queryLogState.errorMessage ?: queryLogState.statusMessage
    LaunchedEffect(queryLogFeedback) {
        val message = queryLogFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissQueryLogFeedback()
    }

    val filtersFeedback = filtersState.errorMessage ?: filtersState.statusMessage
    LaunchedEffect(filtersFeedback) {
        val message = filtersFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissFiltersFeedback()
    }

    val servicesFeedback = blockedServicesState.errorMessage ?: blockedServicesState.statusMessage
    LaunchedEffect(servicesFeedback) {
        val message = servicesFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissBlockedServicesFeedback()
    }

    val settingsFeedback = settingsState.errorMessage ?: settingsState.statusMessage
    LaunchedEffect(settingsFeedback) {
        val message = settingsFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissSettingsFeedback()
    }

    val isRefreshingActiveTab = when (selectedTab) {
        MainTab.Dashboard -> dashboardState.isRefreshing
        MainTab.QueryLog -> queryLogState.isLoading
        MainTab.Filters -> filtersState.isLoading || filtersState.isRefreshingLists
        MainTab.Services -> blockedServicesState.isLoading || blockedServicesState.isSaving
        MainTab.Settings -> settingsState.isLoading
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (selectedTab) {
                                MainTab.Dashboard -> dashboardState.serverHost.ifBlank { "AdDroid Home" }
                                MainTab.QueryLog -> "Query Log"
                                MainTab.Filters -> "Filters"
                                MainTab.Services -> "Blocked Services"
                                MainTab.Settings -> "Settings"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = when (selectedTab) {
                            MainTab.Dashboard -> dashboardState.snapshot?.status?.version
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "AdGuard Home $it" }
                            MainTab.QueryLog,
                            MainTab.Filters,
                            MainTab.Services,
                            MainTab.Settings,
                            -> dashboardState.serverHost.takeIf { it.isNotBlank() }
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (selectedTab) {
                                MainTab.Dashboard -> onRefreshDashboard()
                                MainTab.QueryLog -> onRefreshQueryLog()
                                MainTab.Filters -> onRefreshFiltersStatus()
                                MainTab.Services -> onRefreshBlockedServices()
                                MainTab.Settings -> onRefreshSettings()
                            }
                        },
                        enabled = !isRefreshingActiveTab,
                    ) {
                        Icon(
                            imageVector = refresh,
                            contentDescription = "Refresh",
                        )
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = logout,
                            contentDescription = "Disconnect server",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.Dashboard -> dashboard
                                    MainTab.QueryLog -> receipt_long
                                    MainTab.Filters -> filter_list
                                    MainTab.Services -> apps
                                    MainTab.Settings -> settings
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Dashboard -> DashboardContent(
                state = dashboardState,
                onRefresh = onRefreshDashboard,
                onSelectTimeRange = onSelectStatsTimeRange,
                onToggleProtection = onToggleProtection,
                onPauseProtection = onPauseProtection,
                onOpenQueryLog = onOpenQueryLog,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.QueryLog -> QueryLogContent(
                state = queryLogState,
                onSearchChange = onQueryLogSearchChange,
                onSearchSubmit = onQueryLogSearchSubmit,
                onFilterSelected = onQueryLogFilterSelected,
                onRefresh = onRefreshQueryLog,
                onLoadMore = onLoadMoreQueryLog,
                onSetDomainBlocked = onSetDomainBlocked,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.Filters -> FiltersContent(
                state = filtersState,
                onSelectSection = onFiltersSectionSelected,
                onRefreshStatus = onRefreshFiltersStatus,
                onSetFilteringEnabled = onSetFilteringEnabled,
                onToggleFilterList = onToggleFilterList,
                onAddFilterList = onAddFilterList,
                onRemoveFilterList = onRemoveFilterList,
                onCheckForUpdates = onCheckForFilterUpdates,
                onCustomRulesDraftChange = onCustomRulesDraftChange,
                onDiscardCustomRules = onDiscardCustomRules,
                onSaveCustomRules = onSaveCustomRules,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.Services -> BlockedServicesContent(
                state = blockedServicesState,
                onSearchChange = onBlockedServicesSearchChange,
                onGroupSelected = onBlockedServicesGroupSelected,
                onShowBlockedOnlyChange = onBlockedServicesShowBlockedOnlyChange,
                onToggleService = onToggleBlockedService,
                onRefresh = onRefreshBlockedServices,
                onDiscardChanges = onDiscardBlockedServicesChanges,
                onSaveChanges = onSaveBlockedServices,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.Settings -> SettingsContent(
                state = settingsState,
                onRefresh = onRefreshSettings,
                onSetSafeBrowsingEnabled = onSetSafeBrowsingEnabled,
                onSetParentalEnabled = onSetParentalEnabled,
                onSetSafeSearchEnabled = onSetSafeSearchEnabled,
                onToggleSafeSearchEngine = onToggleSafeSearchEngine,
                onSetRewritesEnabled = onSetRewritesEnabled,
                onAddRewrite = onAddRewrite,
                onUpdateRewrite = onUpdateRewrite,
                onDeleteRewrite = onDeleteRewrite,
                onClearQueryLog = onClearQueryLog,
                onResetStats = onResetStats,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ConnectedHomePreviewHost(
    selectedTab: MainTab,
    queryLogState: QueryLogUiState = QueryLogUiState(),
    settingsState: SettingsUiState = SettingsUiState(),
) {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        ConnectedHomeScreen(
            selectedTab = selectedTab,
            dashboardState = DashboardUiState(
                serverHost = "192.168.1.2:3000",
                snapshot = sampleDashboardSnapshot(protectionEnabled = true),
                selectedTimeRange = StatsTimeRange.Last24Hours,
                defaultWindowMillis = 24L * 3_600_000L,
            ),
            queryLogState = queryLogState,
            filtersState = FiltersUiState(),
            blockedServicesState = BlockedServicesUiState(),
            settingsState = settingsState,
            onSelectTab = {},
            onRefreshDashboard = {},
            onSelectStatsTimeRange = {},
            onToggleProtection = {},
            onPauseProtection = {},
            onOpenQueryLog = { _, _ -> },
            onQueryLogSearchChange = {},
            onQueryLogSearchSubmit = {},
            onQueryLogFilterSelected = {},
            onRefreshQueryLog = {},
            onLoadMoreQueryLog = {},
            onSetDomainBlocked = { _, _ -> },
            onFiltersSectionSelected = {},
            onRefreshFiltersStatus = {},
            onSetFilteringEnabled = {},
            onToggleFilterList = { _, _, _ -> },
            onAddFilterList = { _, _, _ -> },
            onRemoveFilterList = { _, _ -> },
            onCheckForFilterUpdates = {},
            onCustomRulesDraftChange = {},
            onDiscardCustomRules = {},
            onSaveCustomRules = {},
            onBlockedServicesSearchChange = {},
            onBlockedServicesGroupSelected = {},
            onBlockedServicesShowBlockedOnlyChange = {},
            onToggleBlockedService = { _, _ -> },
            onRefreshBlockedServices = {},
            onDiscardBlockedServicesChanges = {},
            onSaveBlockedServices = {},
            onRefreshSettings = {},
            onSetSafeBrowsingEnabled = {},
            onSetParentalEnabled = {},
            onSetSafeSearchEnabled = {},
            onToggleSafeSearchEngine = { _, _ -> },
            onSetRewritesEnabled = {},
            onAddRewrite = { _, _, _ -> },
            onUpdateRewrite = { _, _ -> },
            onDeleteRewrite = {},
            onClearQueryLog = {},
            onResetStats = {},
            onDisconnect = {},
            onDismissDashboardError = {},
            onDismissQueryLogFeedback = {},
            onDismissFiltersFeedback = {},
            onDismissBlockedServicesFeedback = {},
            onDismissSettingsFeedback = {},
        )
    }
}

@Preview(name = "Connected Home Dark", showBackground = true, widthDp = 412, heightDp = 824)
@Composable
private fun ConnectedHomeDarkPreview() {
    ConnectedHomePreviewHost(selectedTab = MainTab.Dashboard)
}

@Preview(name = "Connected Query Log Dark", showBackground = true, widthDp = 412, heightDp = 824)
@Composable
private fun ConnectedHomeQueryLogDarkPreview() {
    val extendedItems = sampleQueryLogItems() + listOf(
        QueryLogItemDto(
            time = "2026-11-26T01:13:41+01:00",
            question = DnsQuestionDto(name = "nas.home.lan", type = "A", dnsClass = "IN"),
            answer = listOf(DnsAnswerDto(type = "A", value = "192.168.1.50", ttl = 10)),
            client = "192.168.1.22",
            clientInfo = QueryLogClientInfoDto(name = "Workstation"),
            elapsedMs = "0.18",
            reason = "Rewrite",
            status = "NOERROR",
        ),
        QueryLogItemDto(
            time = "2026-11-26T01:13:18+01:00",
            question = DnsQuestionDto(name = "telemetry.example.com", type = "A", dnsClass = "IN"),
            client = "192.168.1.40",
            clientInfo = QueryLogClientInfoDto(name = "Living Room TV"),
            elapsedMs = "0.35",
            reason = "FilteredBlackList",
            rules = listOf(ResultRuleDto(filterListId = 1L, text = "||telemetry.example.com^")),
            status = "NOERROR",
        ),
        QueryLogItemDto(
            time = "2026-11-26T01:12:54+01:00",
            question = DnsQuestionDto(name = "android.com", type = "HTTPS", dnsClass = "IN"),
            cached = true,
            upstream = "https://dns.cloudflare.com/dns-query",
            client = "192.168.1.15",
            clientInfo = QueryLogClientInfoDto(name = "Pixel 9"),
            elapsedMs = "0.94",
            reason = "NotFilteredNotFound",
            status = "NOERROR",
        ),
    )
    ConnectedHomePreviewHost(
        selectedTab = MainTab.QueryLog,
        queryLogState = QueryLogUiState(
            items = extendedItems,
            hasLoadedOnce = true,
        ),
    )
}

@Preview(name = "Connected Settings Dark", showBackground = true, widthDp = 412, heightDp = 824)
@Composable
private fun ConnectedHomeSettingsDarkPreview() {
    ConnectedHomePreviewHost(
        selectedTab = MainTab.Settings,
        settingsState = sampleSettingsState(),
    )
}
