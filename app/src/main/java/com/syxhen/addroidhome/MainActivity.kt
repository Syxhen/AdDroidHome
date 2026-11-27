package com.syxhen.addroidhome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.LocalNetworkPolicy
import com.syxhen.addroidhome.ui.AdGuardViewModel
import com.syxhen.addroidhome.ui.AppScreen
import com.syxhen.addroidhome.ui.ConnectedHomeScreen
import com.syxhen.addroidhome.ui.connection.ServerConnectionScreen
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdDroidHomeTheme {
                AdDroidHomeApp()
            }
        }
    }
}

@Composable
fun AdDroidHomeApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: AdGuardViewModel = viewModel(
        factory = AdGuardViewModel.provideFactory(appContext),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.connect()
        } else {
            viewModel.setConnectionError(
                "Local network permission is required on Android 17 to connect to LAN servers. " +
                    "Allow Nearby devices / Local network access and try again.",
            )
        }
    }

    Crossfade(
        targetState = uiState.screen,
        label = "screen_transition",
    ) { screen ->
        when (screen) {
            AppScreen.Connection -> ServerConnectionScreen(
                state = uiState.connection,
                onServerUrlChange = viewModel::onServerUrlChange,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTrustSelfSignedCertsChange = viewModel::onTrustSelfSignedCertsChange,
                onConnect = {
                    if (
                        Build.VERSION.SDK_INT >= ANDROID_17_API_LEVEL &&
                        requiresLocalNetworkPermission(context, uiState.connection.serverUrl)
                    ) {
                        localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    } else {
                        viewModel.connect()
                    }
                },
            )

            AppScreen.Dashboard -> ConnectedHomeScreen(
                selectedTab = uiState.selectedTab,
                dashboardState = uiState.dashboard,
                queryLogState = uiState.queryLog,
                filtersState = uiState.filters,
                blockedServicesState = uiState.blockedServices,
                settingsState = uiState.settings,
                onSelectTab = viewModel::onSelectTab,
                onRefreshDashboard = viewModel::refreshDashboard,
                onSelectStatsTimeRange = viewModel::onSelectStatsTimeRange,
                onToggleProtection = { enabled -> viewModel.setProtection(enabled = enabled) },
                onPauseProtection = { durationMillis ->
                    viewModel.setProtection(enabled = false, durationMillis = durationMillis)
                },
                onOpenQueryLog = viewModel::openQueryLog,
                onQueryLogSearchChange = viewModel::onQueryLogSearchChange,
                onQueryLogSearchSubmit = viewModel::onQueryLogSearchSubmit,
                onQueryLogFilterSelected = viewModel::onQueryLogFilterSelected,
                onRefreshQueryLog = viewModel::refreshQueryLog,
                onLoadMoreQueryLog = viewModel::loadMoreQueryLog,
                onSetDomainBlocked = viewModel::setDomainBlocked,
                onFiltersSectionSelected = viewModel::onFiltersSectionSelected,
                onRefreshFiltersStatus = { viewModel.refreshFiltersStatus() },
                onSetFilteringEnabled = viewModel::setFilteringEnabled,
                onToggleFilterList = viewModel::toggleFilterList,
                onAddFilterList = viewModel::addFilterList,
                onRemoveFilterList = viewModel::removeFilterList,
                onCheckForFilterUpdates = viewModel::updateFilterListsFromUpstream,
                onCustomRulesDraftChange = viewModel::onCustomRulesDraftChange,
                onDiscardCustomRules = viewModel::discardCustomRulesChanges,
                onSaveCustomRules = viewModel::saveCustomRules,
                onBlockedServicesSearchChange = viewModel::onBlockedServicesSearchChange,
                onBlockedServicesGroupSelected = viewModel::onBlockedServicesGroupSelected,
                onBlockedServicesShowBlockedOnlyChange = viewModel::onBlockedServicesShowBlockedOnlyChange,
                onToggleBlockedService = viewModel::onToggleBlockedService,
                onRefreshBlockedServices = { viewModel.refreshBlockedServices() },
                onDiscardBlockedServicesChanges = viewModel::discardBlockedServicesChanges,
                onSaveBlockedServices = viewModel::saveBlockedServices,
                onRefreshSettings = viewModel::refreshSettings,
                onSetSafeBrowsingEnabled = viewModel::setSafeBrowsingEnabled,
                onSetParentalEnabled = viewModel::setParentalEnabled,
                onSetSafeSearchEnabled = viewModel::setSafeSearchEnabled,
                onToggleSafeSearchEngine = viewModel::toggleSafeSearchEngine,
                onSetRewritesEnabled = viewModel::setRewritesEnabled,
                onAddRewrite = viewModel::addRewrite,
                onUpdateRewrite = viewModel::updateRewrite,
                onDeleteRewrite = viewModel::deleteRewrite,
                onClearQueryLog = viewModel::clearQueryLog,
                onResetStats = viewModel::resetStats,
                onDisconnect = viewModel::disconnect,
                onDismissDashboardError = viewModel::dismissDashboardError,
                onDismissQueryLogFeedback = viewModel::dismissQueryLogFeedback,
                onDismissFiltersFeedback = viewModel::dismissFiltersFeedback,
                onDismissBlockedServicesFeedback = viewModel::dismissBlockedServicesFeedback,
                onDismissSettingsFeedback = viewModel::dismissSettingsFeedback,
            )
        }
    }
}

private fun requiresLocalNetworkPermission(
    context: Context,
    rawServerUrl: String,
): Boolean {
    if (Build.VERSION.SDK_INT < ANDROID_17_API_LEVEL) return false
    val normalized = ServerConfig.normalizeBaseUrl(rawServerUrl)
    val host = runCatching { URI(normalized).host }.getOrNull() ?: return false
    if (!LocalNetworkPolicy.isLocalOrPrivateHost(host)) return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_LOCAL_NETWORK,
    ) != PackageManager.PERMISSION_GRANTED
}

private const val ANDROID_17_API_LEVEL = 37
