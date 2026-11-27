package com.syxhen.addroidhome.ui

import com.syxhen.addroidhome.data.model.DashboardSnapshot
import com.syxhen.addroidhome.data.network.model.BlockedServiceDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.ServiceGroupDto
import com.syxhen.addroidhome.data.network.model.formatServiceGroupLabel

enum class AppScreen {
    Connection,
    Dashboard,
}

enum class MainTab(val label: String) {
    Dashboard("Dashboard"),
    QueryLog("Query Log"),
    Filters("Filters"),
    Services("Services"),
    Settings("Settings"),
}

enum class FiltersSection(val label: String) {
    Blocklists("Blocklists"),
    Allowlists("Allowlists"),
    CustomRules("Custom Rules"),
}

data class ServerConnectionUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val trustSelfSignedCerts: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
) {
    val canConnect: Boolean
        get() = serverUrl.isNotBlank() && !isConnecting
}

enum class StatsTimeRange(
    val label: String,
    val recentMillis: Long?,
) {
    Default("Default", null),
    Last1Hour("1h", 3_600_000L),
    Last6Hours("6h", 6 * 3_600_000L),
    Last12Hours("12h", 12 * 3_600_000L),
    Last24Hours("24h", 24 * 3_600_000L),
    Last7Days("7d", 7 * 24 * 3_600_000L),
}

data class DashboardUiState(
    val serverHost: String = "",
    val snapshot: DashboardSnapshot? = null,
    val selectedTimeRange: StatsTimeRange = StatsTimeRange.Default,
    val defaultWindowMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val isUpdatingProtection: Boolean = false,
    val errorMessage: String? = null,
) {
    val availableTimeRanges: List<StatsTimeRange>
        get() = StatsTimeRange.entries.filter { range ->
            val millis = range.recentMillis ?: return@filter true
            val maxWindow = defaultWindowMillis ?: return@filter range != StatsTimeRange.Last7Days
            millis <= maxWindow
        }
}

data class QueryLogUiState(
    val items: List<QueryLogItemDto> = emptyList(),
    val oldestCursor: String? = null,
    val searchQuery: String = "",
    val selectedFilter: QueryLogFilter = QueryLogFilter.All,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val updatingDomain: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class FiltersUiState(
    val status: FilterStatusDto? = null,
    val selectedSection: FiltersSection = FiltersSection.Blocklists,
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val isUpdatingFilteringEnabled: Boolean = false,
    val isRefreshingLists: Boolean = false,
    val togglingFilterUrl: String? = null,
    val deletingFilterUrl: String? = null,
    val isAddingFilter: Boolean = false,
    val customRulesDraft: String = "",
    val isSavingCustomRules: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val savedCustomRulesText: String
        get() = status?.userRules.orEmpty().joinToString("\n")

    val hasUnsavedCustomRules: Boolean
        get() = status != null &&
            normalizeRuleLines(customRulesDraft) != normalizeRuleLines(savedCustomRulesText)

    val activeCustomRulesCount: Int
        get() = status?.userRules.orEmpty().count { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !trimmed.startsWith("!") && !trimmed.startsWith("#")
        }
}

data class BlockedServicesUiState(
    val services: List<BlockedServiceDto> = emptyList(),
    val groups: List<ServiceGroupDto> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val selectedIds: Set<String> = emptySet(),
    val currentSchedule: BlockedServicesScheduleDto? = null,
    val searchQuery: String = "",
    val selectedGroupId: String? = null,
    val showBlockedOnly: Boolean = false,
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val hasUnsavedChanges: Boolean
        get() = hasLoadedOnce && selectedIds != savedIds

    val pendingChangeCount: Int
        get() = (selectedIds - savedIds).size + (savedIds - selectedIds).size

    val blockedCount: Int
        get() = selectedIds.size

    val filteredServices: List<BlockedServiceDto>
        get() {
            val query = searchQuery.trim()
            return services.filter { service ->
                val matchesGroup = selectedGroupId == null || service.groupId == selectedGroupId
                val matchesBlocked = !showBlockedOnly || service.id in selectedIds
                val matchesQuery = query.isEmpty() ||
                    service.displayName.contains(query, ignoreCase = true) ||
                    service.id.contains(query, ignoreCase = true) ||
                    formatServiceGroupLabel(service.groupId).contains(query, ignoreCase = true)
                matchesGroup && matchesBlocked && matchesQuery
            }
        }
}

fun normalizeRuleLines(rawText: String): List<String> =
    rawText
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split('\n')
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }

data class SettingsUiState(
    val safeBrowsingEnabled: Boolean = false,
    val parentalEnabled: Boolean = false,
    val safeSearch: SafeSearchConfigDto = SafeSearchConfigDto(),
    val rewrites: List<RewriteEntryDto> = emptyList(),
    val rewritesEnabled: Boolean? = null,
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val isUpdatingSafeBrowsing: Boolean = false,
    val isUpdatingParental: Boolean = false,
    val isUpdatingSafeSearch: Boolean = false,
    val isUpdatingRewritesEnabled: Boolean = false,
    val isMutatingRewrite: Boolean = false,
    val isClearingQueryLog: Boolean = false,
    val isResettingStats: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class MainUiState(
    val screen: AppScreen = AppScreen.Connection,
    val selectedTab: MainTab = MainTab.Dashboard,
    val connection: ServerConnectionUiState = ServerConnectionUiState(),
    val dashboard: DashboardUiState = DashboardUiState(),
    val queryLog: QueryLogUiState = QueryLogUiState(),
    val filters: FiltersUiState = FiltersUiState(),
    val blockedServices: BlockedServicesUiState = BlockedServicesUiState(),
    val settings: SettingsUiState = SettingsUiState(),
)
