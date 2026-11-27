package com.syxhen.addroidhome.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.SafeSearchEngine
import com.syxhen.addroidhome.data.network.model.StatsDto
import com.syxhen.addroidhome.data.repository.AdGuardRepository
import com.syxhen.addroidhome.data.repository.DefaultAdGuardRepository
import com.syxhen.addroidhome.data.storage.EncryptedServerConfigStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdGuardViewModel(
    private val repository: AdGuardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var dashboardLoadJob: Job? = null
    private var queryLogLoadJob: Job? = null
    private var queryLogSearchDebounceJob: Job? = null
    private var filtersLoadJob: Job? = null
    private var blockedServicesLoadJob: Job? = null
    private var settingsLoadJob: Job? = null

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        val savedConfig = repository.getSavedConfig() ?: return
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    serverUrl = savedConfig.baseUrl,
                    username = savedConfig.username,
                    password = savedConfig.password,
                    trustSelfSignedCerts = savedConfig.trustSelfSignedCerts,
                ),
            )
        }
        connectWithConfig(savedConfig)
    }

    fun onServerUrlChange(value: String) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    serverUrl = value,
                    errorMessage = null,
                ),
            )
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    username = value,
                    errorMessage = null,
                ),
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    password = value,
                    errorMessage = null,
                ),
            )
        }
    }

    fun onTrustSelfSignedCertsChange(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    trustSelfSignedCerts = enabled,
                    errorMessage = null,
                ),
            )
        }
    }

    fun setConnectionError(message: String) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    isConnecting = false,
                    errorMessage = message,
                ),
            )
        }
    }

    fun connect() {
        val form = _uiState.value.connection
        if (form.isConnecting) return
        val config = ServerConfig(
            baseUrl = form.serverUrl,
            username = form.username,
            password = form.password,
            trustSelfSignedCerts = form.trustSelfSignedCerts,
        )
        connectWithConfig(config)
    }

    private fun connectWithConfig(config: ServerConfig) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    isConnecting = true,
                    errorMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            val normalizedConfig = config.copy(baseUrl = ServerConfig.normalizeBaseUrl(config.baseUrl))
            repository.connectAndLoadDashboard(config)
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            screen = AppScreen.Dashboard,
                            selectedTab = MainTab.Dashboard,
                            connection = state.connection.copy(
                                serverUrl = normalizedConfig.baseUrl,
                                isConnecting = false,
                                errorMessage = null,
                            ),
                            dashboard = DashboardUiState(
                                serverHost = normalizedConfig.displayHost,
                                snapshot = snapshot,
                                selectedTimeRange = StatsTimeRange.Default,
                                defaultWindowMillis = inferWindowMillis(snapshot.stats),
                                isRefreshing = false,
                                isUpdatingProtection = false,
                                errorMessage = null,
                            ),
                            queryLog = QueryLogUiState(),
                            filters = FiltersUiState(),
                            blockedServices = BlockedServicesUiState(),
                            settings = SettingsUiState(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            screen = AppScreen.Connection,
                            connection = state.connection.copy(
                                isConnecting = false,
                                errorMessage = error.message ?: "Failed to connect to AdGuard Home.",
                            ),
                        )
                    }
                }
        }
    }

    fun onSelectTab(tab: MainTab) {
        _uiState.update { state -> state.copy(selectedTab = tab) }
        when (tab) {
            MainTab.Dashboard -> Unit
            MainTab.QueryLog -> {
                val logState = _uiState.value.queryLog
                if (!logState.hasLoadedOnce && !logState.isLoading) {
                    refreshQueryLog()
                }
            }
            MainTab.Filters -> {
                val filtersState = _uiState.value.filters
                if (!filtersState.hasLoadedOnce && !filtersState.isLoading) {
                    refreshFiltersStatus()
                }
            }
            MainTab.Services -> {
                val servicesState = _uiState.value.blockedServices
                if (!servicesState.hasLoadedOnce && !servicesState.isLoading) {
                    refreshBlockedServices()
                }
            }
            MainTab.Settings -> {
                val settingsState = _uiState.value.settings
                if (!settingsState.hasLoadedOnce && !settingsState.isLoading) {
                    refreshSettings()
                }
            }
        }
    }

    fun openQueryLog(
        search: String = "",
        filter: QueryLogFilter = QueryLogFilter.All,
    ) {
        queryLogSearchDebounceJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedTab = MainTab.QueryLog,
                queryLog = state.queryLog.copy(
                    searchQuery = search.trim(),
                    selectedFilter = filter,
                    errorMessage = null,
                ),
            )
        }
        refreshQueryLog()
    }

    fun onSelectStatsTimeRange(range: StatsTimeRange) {
        val currentDashboard = _uiState.value.dashboard
        if (currentDashboard.selectedTimeRange == range && !currentDashboard.isRefreshing) {
            return
        }
        val previousRange = currentDashboard.selectedTimeRange
        dashboardLoadJob?.cancel()

        _uiState.update { state ->
            state.copy(
                dashboard = state.dashboard.copy(
                    selectedTimeRange = range,
                    isRefreshing = true,
                    errorMessage = null,
                ),
            )
        }

        dashboardLoadJob = viewModelScope.launch {
            repository.refreshDashboard(recentMillis = range.recentMillis)
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        val nextDefaultWindow = if (range == StatsTimeRange.Default) {
                            inferWindowMillis(snapshot.stats) ?: state.dashboard.defaultWindowMillis
                        } else {
                            state.dashboard.defaultWindowMillis
                        }
                        state.copy(
                            dashboard = state.dashboard.copy(
                                snapshot = snapshot,
                                selectedTimeRange = range,
                                defaultWindowMillis = nextDefaultWindow,
                                isRefreshing = false,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            dashboard = state.dashboard.copy(
                                selectedTimeRange = previousRange,
                                isRefreshing = false,
                                errorMessage = error.message ?: "Unable to load stats for ${range.label}.",
                            ),
                        )
                    }
                }
        }
    }

    fun refreshDashboard() {
        val currentDashboard = _uiState.value.dashboard
        if (currentDashboard.isRefreshing) return
        val activeRange = currentDashboard.selectedTimeRange

        _uiState.update { state ->
            state.copy(
                dashboard = state.dashboard.copy(
                    isRefreshing = true,
                    errorMessage = null,
                ),
            )
        }

        dashboardLoadJob?.cancel()
        dashboardLoadJob = viewModelScope.launch {
            repository.refreshDashboard(recentMillis = activeRange.recentMillis)
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        val nextDefaultWindow = if (activeRange == StatsTimeRange.Default) {
                            inferWindowMillis(snapshot.stats) ?: state.dashboard.defaultWindowMillis
                        } else {
                            state.dashboard.defaultWindowMillis
                        }
                        state.copy(
                            dashboard = state.dashboard.copy(
                                snapshot = snapshot,
                                defaultWindowMillis = nextDefaultWindow,
                                isRefreshing = false,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            dashboard = state.dashboard.copy(
                                isRefreshing = false,
                                errorMessage = error.message ?: "Unable to refresh dashboard.",
                            ),
                        )
                    }
                }
        }
    }

    fun setProtection(enabled: Boolean, durationMillis: Long? = null) {
        val currentDashboard = _uiState.value.dashboard
        if (currentDashboard.isUpdatingProtection) return
        val recentMillis = currentDashboard.selectedTimeRange.recentMillis

        _uiState.update { state ->
            state.copy(
                dashboard = state.dashboard.copy(
                    isUpdatingProtection = true,
                    errorMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setProtection(
                enabled = enabled,
                durationMillis = durationMillis,
                recentMillis = recentMillis,
            )
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            dashboard = state.dashboard.copy(
                                snapshot = snapshot,
                                isUpdatingProtection = false,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            dashboard = state.dashboard.copy(
                                isUpdatingProtection = false,
                                errorMessage = error.message ?: "Failed to update protection state.",
                            ),
                        )
                    }
                }
        }
    }

    fun onQueryLogSearchChange(query: String) {
        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    searchQuery = query,
                    errorMessage = null,
                ),
            )
        }
        queryLogSearchDebounceJob?.cancel()
        queryLogSearchDebounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            refreshQueryLog()
        }
    }

    fun onQueryLogSearchSubmit() {
        queryLogSearchDebounceJob?.cancel()
        refreshQueryLog()
    }

    fun onQueryLogFilterSelected(filter: QueryLogFilter) {
        if (_uiState.value.queryLog.selectedFilter == filter && _uiState.value.queryLog.hasLoadedOnce) {
            return
        }
        queryLogSearchDebounceJob?.cancel()
        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    selectedFilter = filter,
                    errorMessage = null,
                ),
            )
        }
        refreshQueryLog()
    }

    fun refreshQueryLog() {
        queryLogLoadJob?.cancel()
        val search = _uiState.value.queryLog.searchQuery
        val filter = _uiState.value.queryLog.selectedFilter

        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                ),
            )
        }

        queryLogLoadJob = viewModelScope.launch {
            repository.getQueryLog(
                search = search,
                filter = filter,
                olderThan = null,
                limit = AdGuardRepository.DEFAULT_QUERY_LOG_PAGE_SIZE,
            )
                .onSuccess { response ->
                    val nextCursor = response.oldest.ifBlank { response.data.lastOrNull()?.time }
                    val canLoadMore = response.data.size >= AdGuardRepository.DEFAULT_QUERY_LOG_PAGE_SIZE &&
                        !nextCursor.isNullOrBlank()
                    _uiState.update { state ->
                        state.copy(
                            queryLog = state.queryLog.copy(
                                items = response.data,
                                oldestCursor = nextCursor,
                                isLoading = false,
                                isLoadingMore = false,
                                canLoadMore = canLoadMore,
                                hasLoadedOnce = true,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            queryLog = state.queryLog.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                hasLoadedOnce = true,
                                errorMessage = error.message ?: "Unable to load query log.",
                            ),
                        )
                    }
                }
        }
    }

    fun loadMoreQueryLog() {
        val current = _uiState.value.queryLog
        val cursor = current.oldestCursor
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore || cursor.isNullOrBlank()) {
            return
        }

        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    isLoadingMore = true,
                    errorMessage = null,
                ),
            )
        }

        queryLogLoadJob = viewModelScope.launch {
            repository.getQueryLog(
                search = current.searchQuery,
                filter = current.selectedFilter,
                olderThan = cursor,
                limit = AdGuardRepository.DEFAULT_QUERY_LOG_PAGE_SIZE,
            )
                .onSuccess { response ->
                    val nextCursor = response.oldest.ifBlank { response.data.lastOrNull()?.time }
                    val hasMore = response.data.size >= AdGuardRepository.DEFAULT_QUERY_LOG_PAGE_SIZE &&
                        !nextCursor.isNullOrBlank() &&
                        nextCursor != cursor
                    _uiState.update { state ->
                        state.copy(
                            queryLog = state.queryLog.copy(
                                items = state.queryLog.items + response.data,
                                oldestCursor = nextCursor ?: state.queryLog.oldestCursor,
                                isLoadingMore = false,
                                canLoadMore = hasMore,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            queryLog = state.queryLog.copy(
                                isLoadingMore = false,
                                errorMessage = error.message ?: "Unable to load older entries.",
                            ),
                        )
                    }
                }
        }
    }

    fun setDomainBlocked(item: QueryLogItemDto, block: Boolean) {
        val domain = item.displayDomain
        if (domain.isBlank() || _uiState.value.queryLog.updatingDomain != null) return

        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    updatingDomain = domain,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setDomainBlocked(
                domain = domain,
                block = block,
                blockedByCustomRuleOnly = item.isBlockedByCustomRuleOnly,
            )
                .onSuccess {
                    val verb = if (block) "Blocked" else "Unblocked"
                    _uiState.update { state ->
                        val shouldInvalidateFilters = state.filters.hasLoadedOnce &&
                            !state.filters.hasUnsavedCustomRules
                        state.copy(
                            queryLog = state.queryLog.copy(
                                updatingDomain = null,
                                statusMessage = "$verb $domain",
                            ),
                            filters = if (shouldInvalidateFilters) {
                                state.filters.copy(hasLoadedOnce = false)
                            } else {
                                state.filters
                            },
                        )
                    }
                    refreshQueryLog()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            queryLog = state.queryLog.copy(
                                updatingDomain = null,
                                errorMessage = error.message ?: "Failed to update filtering rules.",
                            ),
                        )
                    }
                }
        }
    }

    fun onFiltersSectionSelected(section: FiltersSection) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(selectedSection = section),
            )
        }
    }

    fun refreshFiltersStatus(forceSyncDraft: Boolean = false) {
        filtersLoadJob?.cancel()
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    isLoading = true,
                    errorMessage = null,
                ),
            )
        }

        filtersLoadJob = viewModelScope.launch {
            repository.getFilteringStatus()
                .onSuccess { status ->
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = status,
                                forceSyncDraft = forceSyncDraft || !state.filters.hasLoadedOnce,
                            ).copy(
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = error.message ?: "Unable to load filters.",
                            ),
                        )
                    }
                }
        }
    }

    fun setFilteringEnabled(enabled: Boolean) {
        val currentStatus = _uiState.value.filters.status ?: return
        if (_uiState.value.filters.isUpdatingFilteringEnabled) return

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    isUpdatingFilteringEnabled = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setFilteringEnabled(
                enabled = enabled,
                intervalHours = currentStatus.intervalHours,
            )
                .onSuccess { updatedStatus ->
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = updatedStatus,
                                forceSyncDraft = false,
                            ).copy(
                                isUpdatingFilteringEnabled = false,
                                statusMessage = if (enabled) "DNS filtering enabled" else "DNS filtering disabled",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                isUpdatingFilteringEnabled = false,
                                errorMessage = error.message ?: "Failed to update DNS filtering setting.",
                            ),
                        )
                    }
                }
        }
    }

    fun toggleFilterList(filter: FilterListDto, whitelist: Boolean, enabled: Boolean) {
        if (_uiState.value.filters.togglingFilterUrl != null) return

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    togglingFilterUrl = filter.url,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setFilterListEnabled(
                filter = filter,
                whitelist = whitelist,
                enabled = enabled,
            )
                .onSuccess { updatedStatus ->
                    val action = if (enabled) "Enabled" else "Disabled"
                    val label = filter.name.ifBlank { filter.url }
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = updatedStatus,
                                forceSyncDraft = false,
                            ).copy(
                                togglingFilterUrl = null,
                                statusMessage = "$action $label",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                togglingFilterUrl = null,
                                errorMessage = error.message ?: "Failed to update filter list.",
                            ),
                        )
                    }
                }
        }
    }

    fun addFilterList(name: String, url: String, whitelist: Boolean) {
        if (_uiState.value.filters.isAddingFilter) return

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    isAddingFilter = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.addFilterList(
                name = name,
                url = url,
                whitelist = whitelist,
            )
                .onSuccess { updatedStatus ->
                    val kind = if (whitelist) "allowlist" else "blocklist"
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = updatedStatus,
                                forceSyncDraft = false,
                            ).copy(
                                isAddingFilter = false,
                                statusMessage = "Added $kind \"${name.trim()}\"",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                isAddingFilter = false,
                                errorMessage = error.message ?: "Failed to add filter list.",
                            ),
                        )
                    }
                }
        }
    }

    fun removeFilterList(filter: FilterListDto, whitelist: Boolean) {
        if (_uiState.value.filters.deletingFilterUrl != null) return

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    deletingFilterUrl = filter.url,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.removeFilterList(
                url = filter.url,
                whitelist = whitelist,
            )
                .onSuccess { updatedStatus ->
                    val label = filter.name.ifBlank { filter.url }
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = updatedStatus,
                                forceSyncDraft = false,
                            ).copy(
                                deletingFilterUrl = null,
                                statusMessage = "Removed $label",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                deletingFilterUrl = null,
                                errorMessage = error.message ?: "Failed to remove filter list.",
                            ),
                        )
                    }
                }
        }
    }

    fun updateFilterListsFromUpstream(whitelist: Boolean) {
        if (_uiState.value.filters.isRefreshingLists) return

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    isRefreshingLists = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.refreshFilterLists(whitelist = whitelist)
                .onSuccess { result ->
                    val kind = if (whitelist) "allowlist" else "blocklist"
                    val feedback = when (result.updatedCount) {
                        0 -> "All ${kind}s are up to date"
                        1 -> "Updated 1 $kind"
                        else -> "Updated ${result.updatedCount} ${kind}s"
                    }
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = result.status,
                                forceSyncDraft = false,
                            ).copy(
                                isRefreshingLists = false,
                                statusMessage = feedback,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                isRefreshingLists = false,
                                errorMessage = error.message ?: "Failed to check for filter updates.",
                            ),
                        )
                    }
                }
        }
    }

    fun onCustomRulesDraftChange(value: String) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    customRulesDraft = value,
                    errorMessage = null,
                ),
            )
        }
    }

    fun discardCustomRulesChanges() {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    customRulesDraft = state.filters.savedCustomRulesText,
                    errorMessage = null,
                ),
            )
        }
    }

    fun saveCustomRules() {
        val current = _uiState.value.filters
        if (current.isSavingCustomRules) return
        val normalizedRules = normalizeRuleLines(current.customRulesDraft)

        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    isSavingCustomRules = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setCustomRules(normalizedRules)
                .onSuccess { updatedStatus ->
                    _uiState.update { state ->
                        state.copy(
                            filters = applyFilterStatus(
                                current = state.filters,
                                newStatus = updatedStatus,
                                forceSyncDraft = true,
                            ).copy(
                                isSavingCustomRules = false,
                                statusMessage = "Saved ${updatedStatus.userRules.size} custom rule lines",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                isSavingCustomRules = false,
                                errorMessage = error.message ?: "Failed to save custom rules.",
                            ),
                        )
                    }
                }
        }
    }

    fun dismissDashboardError() {
        _uiState.update { state ->
            state.copy(dashboard = state.dashboard.copy(errorMessage = null))
        }
    }

    fun dismissQueryLogFeedback() {
        _uiState.update { state ->
            state.copy(
                queryLog = state.queryLog.copy(
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }
    }

    fun refreshBlockedServices(forceSyncSelection: Boolean = false) {
        blockedServicesLoadJob?.cancel()
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    isLoading = true,
                    errorMessage = null,
                ),
            )
        }

        blockedServicesLoadJob = viewModelScope.launch {
            repository.getBlockedServicesSnapshot()
                .onSuccess { snapshot ->
                    val serverIds = snapshot.current.ids.toSet()
                    _uiState.update { state ->
                        val current = state.blockedServices
                        val nextSelectedIds = if (forceSyncSelection || !current.hasLoadedOnce || !current.hasUnsavedChanges) {
                            serverIds
                        } else {
                            current.selectedIds
                        }
                        state.copy(
                            blockedServices = current.copy(
                                services = snapshot.services,
                                groups = snapshot.groups,
                                savedIds = serverIds,
                                selectedIds = nextSelectedIds,
                                currentSchedule = snapshot.current,
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            blockedServices = state.blockedServices.copy(
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = error.message ?: "Unable to load blocked services.",
                            ),
                        )
                    }
                }
        }
    }

    fun onBlockedServicesSearchChange(query: String) {
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    searchQuery = query,
                    errorMessage = null,
                ),
            )
        }
    }

    fun onBlockedServicesGroupSelected(groupId: String?) {
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    selectedGroupId = groupId,
                ),
            )
        }
    }

    fun onBlockedServicesShowBlockedOnlyChange(blockedOnly: Boolean) {
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    showBlockedOnly = blockedOnly,
                ),
            )
        }
    }

    fun onToggleBlockedService(serviceId: String, blocked: Boolean) {
        _uiState.update { state ->
            val updated = if (blocked) {
                state.blockedServices.selectedIds + serviceId
            } else {
                state.blockedServices.selectedIds - serviceId
            }
            state.copy(
                blockedServices = state.blockedServices.copy(
                    selectedIds = updated,
                    errorMessage = null,
                ),
            )
        }
    }

    fun discardBlockedServicesChanges() {
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    selectedIds = state.blockedServices.savedIds,
                    errorMessage = null,
                ),
            )
        }
    }

    fun saveBlockedServices() {
        val current = _uiState.value.blockedServices
        if (current.isSaving || !current.hasUnsavedChanges) return

        val desiredIds = current.selectedIds
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    isSaving = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.updateBlockedServices(desiredIds)
                .onSuccess { updatedSchedule ->
                    val confirmedIds = updatedSchedule.ids.toSet()
                    val countLabel = when (confirmedIds.size) {
                        0 -> "No services blocked"
                        1 -> "1 service blocked"
                        else -> "${confirmedIds.size} services blocked"
                    }
                    _uiState.update { state ->
                        state.copy(
                            blockedServices = state.blockedServices.copy(
                                savedIds = confirmedIds,
                                selectedIds = confirmedIds,
                                currentSchedule = updatedSchedule,
                                isSaving = false,
                                statusMessage = "Updated blocked services ($countLabel)",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            blockedServices = state.blockedServices.copy(
                                isSaving = false,
                                errorMessage = error.message ?: "Failed to update blocked services.",
                            ),
                        )
                    }
                }
        }
    }

    fun dismissBlockedServicesFeedback() {
        _uiState.update { state ->
            state.copy(
                blockedServices = state.blockedServices.copy(
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }
    }

    fun dismissFiltersFeedback() {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }
    }

    fun refreshSettings() {
        settingsLoadJob?.cancel()
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isLoading = true,
                    errorMessage = null,
                ),
            )
        }

        settingsLoadJob = viewModelScope.launch {
            repository.getSettingsSnapshot()
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                safeBrowsingEnabled = snapshot.safeBrowsingEnabled,
                                parentalEnabled = snapshot.parentalEnabled,
                                safeSearch = snapshot.safeSearch,
                                rewrites = snapshot.rewrites,
                                rewritesEnabled = snapshot.rewritesEnabled,
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isLoading = false,
                                hasLoadedOnce = true,
                                errorMessage = error.message ?: "Unable to load AdGuard Home settings.",
                            ),
                        )
                    }
                }
        }
    }

    fun setSafeBrowsingEnabled(enabled: Boolean) {
        if (_uiState.value.settings.isUpdatingSafeBrowsing) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isUpdatingSafeBrowsing = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setSafeBrowsingEnabled(enabled)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                safeBrowsingEnabled = updated,
                                isUpdatingSafeBrowsing = false,
                                statusMessage = if (updated) "Safe Browsing enabled" else "Safe Browsing disabled",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isUpdatingSafeBrowsing = false,
                                errorMessage = error.message ?: "Failed to update Safe Browsing.",
                            ),
                        )
                    }
                }
        }
    }

    fun setParentalEnabled(enabled: Boolean) {
        if (_uiState.value.settings.isUpdatingParental) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isUpdatingParental = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setParentalEnabled(enabled)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                parentalEnabled = updated,
                                isUpdatingParental = false,
                                statusMessage = if (updated) "Parental Control enabled" else "Parental Control disabled",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isUpdatingParental = false,
                                errorMessage = error.message ?: "Failed to update Parental Control.",
                            ),
                        )
                    }
                }
        }
    }

    fun setSafeSearchEnabled(enabled: Boolean) {
        val current = _uiState.value.settings.safeSearch
        val updatedConfig = if (enabled && current.enabledEngineCount == 0) {
            current.withAllEnginesEnabled().copy(enabled = true)
        } else {
            current.copy(enabled = enabled)
        }
        updateSafeSearchConfig(
            config = updatedConfig,
            successMessage = if (enabled) "Safe Search enabled" else "Safe Search disabled",
        )
    }

    fun toggleSafeSearchEngine(engine: SafeSearchEngine, enabled: Boolean) {
        val current = _uiState.value.settings.safeSearch
        val updatedConfig = current.withEngineEnabled(engine, enabled)
        updateSafeSearchConfig(
            config = updatedConfig,
            successMessage = if (enabled) {
                "Safe Search enabled for ${engine.label}"
            } else {
                "Safe Search disabled for ${engine.label}"
            },
        )
    }

    private fun updateSafeSearchConfig(
        config: SafeSearchConfigDto,
        successMessage: String,
    ) {
        if (_uiState.value.settings.isUpdatingSafeSearch) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isUpdatingSafeSearch = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.updateSafeSearch(config)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                safeSearch = updated,
                                isUpdatingSafeSearch = false,
                                statusMessage = successMessage,
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isUpdatingSafeSearch = false,
                                errorMessage = error.message ?: "Failed to update Safe Search settings.",
                            ),
                        )
                    }
                }
        }
    }

    fun setRewritesEnabled(enabled: Boolean) {
        if (_uiState.value.settings.isUpdatingRewritesEnabled) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isUpdatingRewritesEnabled = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.setRewritesEnabled(enabled)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                rewritesEnabled = updated,
                                isUpdatingRewritesEnabled = false,
                                statusMessage = if (updated) "DNS rewrites enabled" else "DNS rewrites disabled",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isUpdatingRewritesEnabled = false,
                                errorMessage = error.message ?: "Failed to update DNS rewrites setting.",
                            ),
                        )
                    }
                }
        }
    }

    fun addRewrite(domain: String, answer: String, enabled: Boolean = true) {
        if (_uiState.value.settings.isMutatingRewrite) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isMutatingRewrite = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.addRewrite(domain = domain, answer = answer, enabled = enabled)
                .onSuccess { updatedList ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                rewrites = updatedList,
                                isMutatingRewrite = false,
                                statusMessage = "Added rewrite for ${domain.trim()}",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isMutatingRewrite = false,
                                errorMessage = error.message ?: "Failed to add DNS rewrite.",
                            ),
                        )
                    }
                }
        }
    }

    fun updateRewrite(target: RewriteEntryDto, updated: RewriteEntryDto) {
        if (_uiState.value.settings.isMutatingRewrite) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isMutatingRewrite = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.updateRewrite(target = target, updated = updated)
                .onSuccess { updatedList ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                rewrites = updatedList,
                                isMutatingRewrite = false,
                                statusMessage = "Updated rewrite for ${updated.domain.trim()}",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isMutatingRewrite = false,
                                errorMessage = error.message ?: "Failed to update DNS rewrite.",
                            ),
                        )
                    }
                }
        }
    }

    fun deleteRewrite(entry: RewriteEntryDto) {
        if (_uiState.value.settings.isMutatingRewrite) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isMutatingRewrite = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.deleteRewrite(entry)
                .onSuccess { updatedList ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                rewrites = updatedList,
                                isMutatingRewrite = false,
                                statusMessage = "Deleted rewrite for ${entry.domain}",
                                errorMessage = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isMutatingRewrite = false,
                                errorMessage = error.message ?: "Failed to delete DNS rewrite.",
                            ),
                        )
                    }
                }
        }
    }

    fun clearQueryLog() {
        if (_uiState.value.settings.isClearingQueryLog) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isClearingQueryLog = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.clearQueryLog()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isClearingQueryLog = false,
                                statusMessage = "Query log cleared",
                                errorMessage = null,
                            ),
                            queryLog = state.queryLog.copy(
                                items = emptyList(),
                                oldestCursor = null,
                                canLoadMore = false,
                                hasLoadedOnce = false,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isClearingQueryLog = false,
                                errorMessage = error.message ?: "Failed to clear query log.",
                            ),
                        )
                    }
                }
        }
    }

    fun resetStats() {
        if (_uiState.value.settings.isResettingStats) return
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    isResettingStats = true,
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            repository.resetStats()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isResettingStats = false,
                                statusMessage = "Statistics reset to zero",
                                errorMessage = null,
                            ),
                        )
                    }
                    refreshDashboard()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            settings = state.settings.copy(
                                isResettingStats = false,
                                errorMessage = error.message ?: "Failed to reset statistics.",
                            ),
                        )
                    }
                }
        }
    }

    fun dismissSettingsFeedback() {
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    errorMessage = null,
                    statusMessage = null,
                ),
            )
        }
    }

    fun disconnect() {
        dashboardLoadJob?.cancel()
        queryLogLoadJob?.cancel()
        queryLogSearchDebounceJob?.cancel()
        filtersLoadJob?.cancel()
        blockedServicesLoadJob?.cancel()
        settingsLoadJob?.cancel()
        repository.disconnect()
        _uiState.update { state ->
            MainUiState(
                screen = AppScreen.Connection,
                selectedTab = MainTab.Dashboard,
                connection = ServerConnectionUiState(
                    serverUrl = state.connection.serverUrl,
                    username = state.connection.username,
                    password = "",
                    trustSelfSignedCerts = state.connection.trustSelfSignedCerts,
                ),
                dashboard = DashboardUiState(),
                queryLog = QueryLogUiState(),
                filters = FiltersUiState(),
                blockedServices = BlockedServicesUiState(),
                settings = SettingsUiState(),
            )
        }
    }

    private fun applyFilterStatus(
        current: FiltersUiState,
        newStatus: FilterStatusDto,
        forceSyncDraft: Boolean,
    ): FiltersUiState {
        val nextDraft = if (forceSyncDraft || !current.hasUnsavedCustomRules) {
            newStatus.userRules.joinToString("\n")
        } else {
            current.customRulesDraft
        }
        return current.copy(
            status = newStatus,
            customRulesDraft = nextDraft,
        )
    }

    private fun inferWindowMillis(stats: StatsDto): Long? {
        val bucketCount = stats.dnsQueries.size.toLong()
        if (bucketCount <= 0L) return null
        return when (stats.timeUnits.lowercase()) {
            "days" -> bucketCount * 24L * 3_600_000L
            "hours" -> bucketCount * 3_600_000L
            else -> null
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 350L

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repository = DefaultAdGuardRepository(
                        storage = EncryptedServerConfigStorage(appContext),
                    )
                    return AdGuardViewModel(repository) as T
                }
            }
        }
    }
}
