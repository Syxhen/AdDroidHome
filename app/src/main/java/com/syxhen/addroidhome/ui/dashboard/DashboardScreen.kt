package com.syxhen.addroidhome.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.data.model.DashboardSnapshot
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.StatsDto
import com.syxhen.addroidhome.data.network.model.TopCountEntry
import com.syxhen.addroidhome.data.network.model.toTopCountEntries
import com.syxhen.addroidhome.data.network.model.toTopLatencyEntries
import com.syxhen.addroidhome.ui.DashboardUiState
import com.syxhen.addroidhome.ui.StatsTimeRange
import com.syxhen.addroidhome.ui.icons.chevron_right
import com.syxhen.addroidhome.ui.icons.gpp_maybe
import com.syxhen.addroidhome.ui.icons.logout
import com.syxhen.addroidhome.ui.icons.refresh
import com.syxhen.addroidhome.ui.icons.shield
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

private enum class TopListTab(val label: String) {
    Blocked("Blocked"),
    Queried("Queried"),
    Clients("Clients"),
}

private data class PauseOption(
    val label: String,
    val durationMillis: Long,
)

private data class UpstreamStatItem(
    val rawName: String,
    val displayName: String,
    val protocolTag: String?,
    val responses: Long,
    val sharePercent: Double,
    val avgLatencyMillis: Double?,
)

private val QuickPauseOptions = listOf(
    PauseOption("5 min", 5 * 60_000L),
    PauseOption("15 min", 15 * 60_000L),
    PauseOption("1 hour", 60 * 60_000L),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onSelectTimeRange: (StatsTimeRange) -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onPauseProtection: (Long) -> Unit,
    onOpenQueryLog: (search: String, filter: QueryLogFilter) -> Unit,
    onDisconnect: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissError()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.serverHost.ifBlank { "AdDroid Home" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val version = state.snapshot?.status?.version.orEmpty()
                        if (version.isNotBlank()) {
                            Text(
                                text = "AdGuard Home $version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                    ) {
                        Icon(
                            imageVector = refresh,
                            contentDescription = "Refresh dashboard",
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
    ) { innerPadding ->
        DashboardContent(
            state = state,
            onRefresh = onRefresh,
            onSelectTimeRange = onSelectTimeRange,
            onToggleProtection = onToggleProtection,
            onPauseProtection = onPauseProtection,
            onOpenQueryLog = onOpenQueryLog,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onSelectTimeRange: (StatsTimeRange) -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onPauseProtection: (Long) -> Unit,
    onOpenQueryLog: (search: String, filter: QueryLogFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TopListTab.Blocked) }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        val snapshot = state.snapshot
        if (snapshot == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Pull down to load AdGuard Home statistics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val upstreamItems = remember(snapshot.stats) {
                buildUpstreamStatItems(snapshot.stats)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "protection") {
                    ProtectionStatusCard(
                        status = snapshot.status,
                        isUpdating = state.isUpdatingProtection,
                        onToggleProtection = onToggleProtection,
                        onPauseProtection = onPauseProtection,
                    )
                }

                item(key = "time_range") {
                    StatsTimeRangeSelector(
                        availableRanges = state.availableTimeRanges,
                        selectedRange = state.selectedTimeRange,
                        stats = snapshot.stats,
                        onSelectRange = onSelectTimeRange,
                    )
                }

                item(key = "summary_metrics") {
                    SummaryMetricsGrid(
                        stats = snapshot.stats,
                        onOpenQueryLog = onOpenQueryLog,
                    )
                }

                if (snapshot.stats.dnsQueries.isNotEmpty()) {
                    item(key = "activity_chart") {
                        QueryActivityCard(stats = snapshot.stats)
                    }
                }

                if (upstreamItems.isNotEmpty()) {
                    item(key = "upstreams_card") {
                        UpstreamsCard(items = upstreamItems)
                    }
                }

                item(key = "top_lists") {
                    TopListsCard(
                        stats = snapshot.stats,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onOpenQueryLog = onOpenQueryLog,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsTimeRangeSelector(
    availableRanges: List<StatsTimeRange>,
    selectedRange: StatsTimeRange,
    stats: StatsDto,
    onSelectRange: (StatsTimeRange) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Stats Window",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stats.dnsQueries.isNotEmpty()) {
                Text(
                    text = "Showing ${stats.dnsQueries.size} ${stats.timeUnits}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            availableRanges.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { onSelectRange(range) },
                    label = { Text(range.label) },
                )
            }
        }
    }
}

@Composable
private fun ProtectionStatusCard(
    status: ServerStatusDto,
    isUpdating: Boolean,
    onToggleProtection: (Boolean) -> Unit,
    onPauseProtection: (Long) -> Unit,
) {
    val isProtected = status.protectionEnabled
    val isPausedTemporarily = !isProtected && status.protectionDisabledDurationMillis > 0L

    val containerColor = if (isProtected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isProtected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (isProtected) shield else gpp_maybe,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isProtected -> "Protection Active"
                            isPausedTemporarily -> "Protection Paused"
                            else -> "Protection Disabled"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            isProtected -> "DNS server on port ${status.dnsPort}"
                            isPausedTemporarily -> "Resumes in ${formatDuration(status.protectionDisabledDurationMillis)}"
                            else -> "Tap the switch to re-enable DNS filtering"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.85f),
                    )
                }

                Switch(
                    checked = isProtected,
                    onCheckedChange = onToggleProtection,
                    enabled = !isUpdating,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle DNS protection"
                    },
                )
            }

            if (isProtected) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pause:",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.85f),
                    )
                    QuickPauseOptions.forEach { option ->
                        AssistChip(
                            onClick = { onPauseProtection(option.durationMillis) },
                            enabled = !isUpdating,
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricsGrid(
    stats: StatsDto,
    onOpenQueryLog: (search: String, filter: QueryLogFilter) -> Unit,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val otherProtections = stats.numReplacedSafeBrowsing +
        stats.numReplacedParental +
        stats.numReplacedSafeSearch

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "DNS Queries",
                value = numberFormat.format(stats.numDnsQueries),
                subtitle = if (stats.dnsQueries.isNotEmpty()) {
                    "Last ${stats.dnsQueries.size} ${stats.timeUnits}"
                } else {
                    "View all queries"
                },
                onClick = { onOpenQueryLog("", QueryLogFilter.All) },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Blocked",
                value = numberFormat.format(stats.numBlockedFiltering),
                subtitle = String.format(Locale.US, "%.1f%% of queries", stats.blockedPercentage),
                highlightValue = true,
                onClick = { onOpenQueryLog("", QueryLogFilter.Blocked) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Avg Latency",
                value = String.format(Locale.US, "%.1f ms", stats.avgProcessingTimeMillis),
                subtitle = "Inspect query log",
                onClick = { onOpenQueryLog("", QueryLogFilter.All) },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Safe & Parental",
                value = numberFormat.format(otherProtections),
                subtitle = "SB ${stats.numReplacedSafeBrowsing} · Par ${stats.numReplacedParental} · SS ${stats.numReplacedSafeSearch}",
                onClick = { onOpenQueryLog("", QueryLogFilter.SafeAndParental) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightValue: Boolean = false,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = chevron_right,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (highlightValue) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QueryActivityCard(stats: StatsDto) {
    val totalColor = MaterialTheme.colorScheme.secondaryContainer
    val blockedColor = MaterialTheme.colorScheme.primary
    val highlightBandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val numberFormat = remember { NumberFormat.getIntegerInstance() }

    val queries = stats.dnsQueries
    val blocked = stats.blockedFiltering
    val maxValue = max(1L, queries.maxOrNull() ?: 1L)
    var selectedIndex by remember(stats) { mutableStateOf<Int?>(null) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Query Activity",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDot(label = "Total", color = totalColor)
                    LegendDot(label = "Blocked", color = blockedColor)
                }
            }

            val activeIndex = selectedIndex?.takeIf { it in queries.indices }
            if (activeIndex != null) {
                val bucketQueries = queries[activeIndex]
                val bucketBlocked = blocked.getOrElse(activeIndex) { 0L }.coerceAtMost(bucketQueries)
                val bucketPercent = if (bucketQueries == 0L) {
                    0.0
                } else {
                    (bucketBlocked.toDouble() / bucketQueries.toDouble()) * 100.0
                }
                val bucketLabel = formatBucketLabel(
                    index = activeIndex,
                    totalBuckets = queries.size,
                    timeUnits = stats.timeUnits,
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = bucketLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Total %s · Blocked %s (%.1f%%)",
                                    numberFormat.format(bucketQueries),
                                    numberFormat.format(bucketBlocked),
                                    bucketPercent,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { selectedIndex = null }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            } else {
                val unitLabel = if (stats.timeUnits.equals("days", ignoreCase = true)) "day" else "hr"
                Text(
                    text = "Peak ${numberFormat.format(maxValue)}/$unitLabel · Tap or drag chart to inspect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .semantics {
                        contentDescription =
                            "Query activity bar chart over ${queries.size} ${stats.timeUnits}, peak $maxValue queries"
                    }
                    .pointerInput(queries.size) {
                        detectTapGestures { offset ->
                            val tapped = barIndexForX(
                                x = offset.x,
                                width = size.width.toFloat(),
                                count = queries.size,
                            )
                            selectedIndex = if (selectedIndex == tapped) null else tapped
                        }
                    }
                    .pointerInput(queries.size) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = barIndexForX(
                                    x = offset.x,
                                    width = size.width.toFloat(),
                                    count = queries.size,
                                )
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                selectedIndex = barIndexForX(
                                    x = change.position.x,
                                    width = size.width.toFloat(),
                                    count = queries.size,
                                )
                            },
                        )
                    },
            ) {
                val count = queries.size
                if (count == 0) return@Canvas

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )

                val gap = if (count > 24) 2.dp.toPx() else 4.dp.toPx()
                val totalGap = gap * (count - 1).coerceAtLeast(0)
                val barWidth = ((size.width - totalGap) / count).coerceAtLeast(2f)
                val corner = CornerRadius(barWidth / 3f, barWidth / 3f)
                val topPadding = 6.dp.toPx()
                val usableHeight = (size.height - topPadding).coerceAtLeast(1f)

                queries.forEachIndexed { index, queryCount ->
                    val left = index * (barWidth + gap)
                    val isSelected = activeIndex == index
                    val alphaMultiplier = if (activeIndex == null || isSelected) 1f else 0.4f

                    if (isSelected) {
                        drawRoundRect(
                            color = highlightBandColor,
                            topLeft = Offset((left - gap / 2f).coerceAtLeast(0f), 0f),
                            size = Size(barWidth + gap, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        )
                    }

                    val totalHeight = (queryCount.toFloat() / maxValue.toFloat()) * usableHeight
                    if (totalHeight > 0f) {
                        drawRoundRect(
                            color = totalColor.copy(alpha = totalColor.alpha * alphaMultiplier),
                            topLeft = Offset(left, size.height - totalHeight),
                            size = Size(barWidth, totalHeight),
                            cornerRadius = corner,
                        )
                    }

                    val blockedCount = blocked.getOrElse(index) { 0L }.coerceAtMost(queryCount)
                    val blockedHeight = (blockedCount.toFloat() / maxValue.toFloat()) * usableHeight
                    if (blockedHeight > 0f) {
                        drawRoundRect(
                            color = blockedColor.copy(alpha = blockedColor.alpha * alphaMultiplier),
                            topLeft = Offset(left, size.height - blockedHeight),
                            size = Size(barWidth, blockedHeight),
                            cornerRadius = corner,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatAxisBucketLabel(0, queries.size, stats.timeUnits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (queries.size >= 4) {
                    Text(
                        text = formatAxisBucketLabel(queries.size / 2, queries.size, stats.timeUnits),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatAxisBucketLabel(queries.lastIndex, queries.size, stats.timeUnits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpstreamsCard(items: List<UpstreamStatItem>) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
    )
    var selectedUpstream by remember(items) { mutableStateOf<String?>(null) }
    val totalResponses = remember(items) { items.sumOf { it.responses }.coerceAtLeast(1L) }
    val maxLatencyMillis = remember(items) {
        items.mapNotNull { it.avgLatencyMillis }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Upstreams & Latency",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Tap a segment or server to inspect details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selectedUpstream != null) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { selectedUpstream = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (items.any { it.responses > 0L }) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(CircleShape)
                        .semantics {
                            contentDescription = "Upstream DNS response distribution bar across ${items.size} servers"
                        }
                        .pointerInput(items) {
                            detectTapGestures { offset ->
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val fraction = (offset.x / width).coerceIn(0f, 1f)
                                var cumulative = 0f
                                val hit = items.firstOrNull { item ->
                                    cumulative += item.responses.toFloat() / totalResponses.toFloat()
                                    fraction <= cumulative
                                } ?: items.lastOrNull()
                                if (hit != null) {
                                    selectedUpstream = if (selectedUpstream == hit.rawName) null else hit.rawName
                                }
                            }
                        },
                ) {
                    val gapPx = 2.dp.toPx()
                    val nonZeroItems = items.withIndex().filter { it.value.responses > 0L }
                    val usableWidth = (size.width - gapPx * (nonZeroItems.size - 1).coerceAtLeast(0))
                        .coerceAtLeast(1f)
                    var startX = 0f

                    nonZeroItems.forEach { (index, item) ->
                        val segmentWidth = (item.responses.toFloat() / totalResponses.toFloat()) * usableWidth
                        val baseColor = palette[index % palette.size]
                        val isSelected = selectedUpstream == null || selectedUpstream == item.rawName
                        val color = if (isSelected) baseColor else baseColor.copy(alpha = 0.35f)
                        drawRect(
                            color = color,
                            topLeft = Offset(startX, 0f),
                            size = Size(segmentWidth.coerceAtLeast(2f), size.height),
                        )
                        startX += segmentWidth + gapPx
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { index, item ->
                    val accentColor = palette[index % palette.size]
                    val isSelected = selectedUpstream == item.rawName
                    val rowBackground = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        Color.Transparent
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(rowBackground)
                            .clickable {
                                selectedUpstream = if (isSelected) null else item.rawName
                            }
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                            )
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (item.avgLatencyMillis != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f ms", item.avgLatencyMillis),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (item.responses > 0L) {
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", item.sharePercent),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (item.avgLatencyMillis != null) {
                            LinearProgressIndicator(
                                progress = {
                                    (item.avgLatencyMillis / maxLatencyMillis).toFloat().coerceIn(0.04f, 1f)
                                },
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                            )
                        }

                        if (isSelected) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.rawName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val detailParts = buildList {
                                    item.protocolTag?.let { add(it) }
                                    if (item.responses > 0L) {
                                        add("${numberFormat.format(item.responses)} responses (${String.format(Locale.US, "%.1f%%", item.sharePercent)})")
                                    }
                                    item.avgLatencyMillis?.let {
                                        add(String.format(Locale.US, "Avg %.2f ms", it))
                                    }
                                }
                                Text(
                                    text = detailParts.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopListsCard(
    stats: StatsDto,
    selectedTab: TopListTab,
    onSelectTab: (TopListTab) -> Unit,
    onOpenQueryLog: (search: String, filter: QueryLogFilter) -> Unit,
) {
    val entries: List<TopCountEntry> = remember(stats, selectedTab) {
        when (selectedTab) {
            TopListTab.Blocked -> stats.topBlockedDomains.toTopCountEntries()
            TopListTab.Queried -> stats.topQueriedDomains.toTopCountEntries()
            TopListTab.Clients -> stats.topClients.toTopCountEntries()
        }.take(6)
    }
    val maxCount = max(1L, entries.maxOfOrNull { it.count } ?: 1L)
    val numberFormat = remember { NumberFormat.getIntegerInstance() }

    val targetFilter = when (selectedTab) {
        TopListTab.Blocked -> QueryLogFilter.Blocked
        TopListTab.Queried,
        TopListTab.Clients,
        -> QueryLogFilter.All
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Top Activity",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Tap an entry to filter Query Log",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TopListTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        label = { Text(tab.label) },
                    )
                }
            }

            if (entries.isEmpty()) {
                Text(
                    text = "No entries recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entries.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onOpenQueryLog(entry.name, targetFilter) }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Text(
                                    text = numberFormat.format(entry.count),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Icon(
                                    imageVector = chevron_right,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            LinearProgressIndicator(
                                progress = { entry.count.toFloat() / maxCount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun barIndexForX(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val normalized = (x / width).coerceIn(0f, 0.9999f)
    return (normalized * count).toInt().coerceIn(0, count - 1)
}

private fun formatBucketLabel(index: Int, totalBuckets: Int, timeUnits: String): String {
    val ago = (totalBuckets - 1 - index).coerceAtLeast(0)
    val isDays = timeUnits.equals("days", ignoreCase = true)
    return when {
        ago == 0 && isDays -> "Today"
        ago == 0 -> "Current hour"
        ago == 1 && isDays -> "Yesterday"
        isDays -> "${ago}d ago"
        else -> "${ago}h ago"
    }
}

private fun formatAxisBucketLabel(index: Int, totalBuckets: Int, timeUnits: String): String {
    val ago = (totalBuckets - 1 - index).coerceAtLeast(0)
    val isDays = timeUnits.equals("days", ignoreCase = true)
    return when {
        ago == 0 && isDays -> "Today"
        ago == 0 -> "Now"
        isDays -> "${ago}d ago"
        else -> "${ago}h ago"
    }
}

private fun buildUpstreamStatItems(stats: StatsDto): List<UpstreamStatItem> {
    val responseMap = linkedMapOf<String, Long>()
    stats.topUpstreamsResponses.toTopCountEntries().forEach { entry ->
        responseMap[entry.name] = (responseMap[entry.name] ?: 0L) + entry.count
    }

    val latencyMap = linkedMapOf<String, Double>()
    stats.topUpstreamsAvgTime.toTopLatencyEntries().forEach { entry ->
        latencyMap[entry.name] = entry.avgTimeMillis
    }

    val allNames = (responseMap.keys + latencyMap.keys).distinct()
    if (allNames.isEmpty()) return emptyList()

    val totalResponses = responseMap.values.sum().coerceAtLeast(1L)
    return allNames
        .map { raw ->
            val responses = responseMap[raw] ?: 0L
            val (displayName, protocolTag) = parseUpstreamDisplay(raw)
            UpstreamStatItem(
                rawName = raw,
                displayName = displayName,
                protocolTag = protocolTag,
                responses = responses,
                sharePercent = (responses.toDouble() / totalResponses.toDouble()) * 100.0,
                avgLatencyMillis = latencyMap[raw],
            )
        }
        .sortedWith(
            compareByDescending<UpstreamStatItem> { it.responses }
                .thenBy { it.avgLatencyMillis ?: Double.MAX_VALUE },
        )
        .take(6)
}

private fun parseUpstreamDisplay(raw: String): Pair<String, String?> {
    val trimmed = raw.trim()
    val protocolTag = when {
        trimmed.startsWith("https://", ignoreCase = true) -> "DoH"
        trimmed.startsWith("h3://", ignoreCase = true) -> "DoH3"
        trimmed.startsWith("tls://", ignoreCase = true) -> "DoT"
        trimmed.startsWith("quic://", ignoreCase = true) -> "DoQ"
        trimmed.startsWith("sdns://", ignoreCase = true) -> "DNSCrypt"
        trimmed.startsWith("tcp://", ignoreCase = true) -> "TCP"
        trimmed.startsWith("udp://", ignoreCase = true) -> "UDP"
        else -> null
    }
    val cleaned = trimmed
        .substringAfter("://", trimmed)
        .substringBefore("/dns-query")
        .trimEnd('/')
        .ifBlank { trimmed }
    return cleaned to protocolTag
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

internal fun sampleDashboardSnapshot(protectionEnabled: Boolean = true): DashboardSnapshot {
    return DashboardSnapshot(
        status = ServerStatusDto(
            dnsAddresses = listOf("192.168.1.2"),
            dnsPort = 53,
            httpPort = 3000,
            protectionEnabled = protectionEnabled,
            protectionDisabledDurationMillis = if (protectionEnabled) 0L else 272_000L,
            running = true,
            version = "v0.107.72",
            language = "en",
        ),
        stats = StatsDto(
            timeUnits = "hours",
            numDnsQueries = 48_920,
            numBlockedFiltering = 11_480,
            numReplacedSafeBrowsing = 4,
            numReplacedSafeSearch = 19,
            numReplacedParental = 7,
            avgProcessingTimeSeconds = 0.0142,
            dnsQueries = listOf(1200, 1450, 980, 1800, 2600, 3100, 2900, 2400, 3500, 3900, 3200, 2850),
            blockedFiltering = listOf(280, 340, 210, 420, 680, 790, 710, 560, 890, 940, 770, 640),
            topBlockedDomains = listOf(
                mapOf("telemetry.example.com" to 1840L),
                mapOf("ads.tracker.net" to 1320L),
                mapOf("metrics.analytics.io" to 960L),
                mapOf("pixel.adnetwork.org" to 640L),
            ),
            topQueriedDomains = listOf(
                mapOf("github.com" to 2410L),
                mapOf("android.com" to 1890L),
                mapOf("cloudflare-dns.com" to 1450L),
            ),
            topClients = listOf(
                mapOf("192.168.1.15" to 18_400L),
                mapOf("192.168.1.22" to 12_150L),
                mapOf("192.168.1.40" to 8_910L),
            ),
            topUpstreamsResponses = listOf(
                mapOf("https://dns.cloudflare.com/dns-query" to 24_120L),
                mapOf("tls://dns.quad9.net" to 11_340L),
                mapOf("quic://dns.adguard-dns.com" to 5_890L),
            ),
            topUpstreamsAvgTime = listOf(
                mapOf("https://dns.cloudflare.com/dns-query" to 0.0124),
                mapOf("tls://dns.quad9.net" to 0.0198),
                mapOf("quic://dns.adguard-dns.com" to 0.0245),
            ),
        ),
    )
}

@Preview(name = "Dashboard Light", showBackground = true)
@Composable
private fun DashboardScreenLightPreview() {
    AdDroidHomeTheme(dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                serverHost = "192.168.1.2:3000",
                snapshot = sampleDashboardSnapshot(protectionEnabled = true),
                selectedTimeRange = StatsTimeRange.Last24Hours,
                defaultWindowMillis = 24L * 3_600_000L,
            ),
            onRefresh = {},
            onSelectTimeRange = {},
            onToggleProtection = {},
            onPauseProtection = {},
            onOpenQueryLog = { _, _ -> },
            onDisconnect = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "Dashboard Paused Dark", showBackground = true)
@Composable
private fun DashboardScreenPausedDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                serverHost = "adguard.home.local",
                snapshot = sampleDashboardSnapshot(protectionEnabled = false),
                selectedTimeRange = StatsTimeRange.Default,
                defaultWindowMillis = 7L * 24L * 3_600_000L,
            ),
            onRefresh = {},
            onSelectTimeRange = {},
            onToggleProtection = {},
            onPauseProtection = {},
            onOpenQueryLog = { _, _ -> },
            onDisconnect = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "Upstreams & Top Lists Light", showBackground = true)
@Composable
private fun UpstreamsAndTopListsPreview() {
    val snapshot = sampleDashboardSnapshot(protectionEnabled = true)
    AdDroidHomeTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UpstreamsCard(items = buildUpstreamStatItems(snapshot.stats))
            TopListsCard(
                stats = snapshot.stats,
                selectedTab = TopListTab.Blocked,
                onSelectTab = {},
                onOpenQueryLog = { _, _ -> },
            )
        }
    }
}
