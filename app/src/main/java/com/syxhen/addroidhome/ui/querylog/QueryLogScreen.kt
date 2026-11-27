package com.syxhen.addroidhome.ui.querylog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.data.network.model.DnsAnswerDto
import com.syxhen.addroidhome.data.network.model.DnsQuestionDto
import com.syxhen.addroidhome.data.network.model.QueryLogClientInfoDto
import com.syxhen.addroidhome.data.network.model.QueryLogFilter
import com.syxhen.addroidhome.data.network.model.QueryLogItemDto
import com.syxhen.addroidhome.data.network.model.ResultRuleDto
import com.syxhen.addroidhome.ui.QueryLogUiState
import com.syxhen.addroidhome.ui.icons.block
import com.syxhen.addroidhome.ui.icons.check_circle
import com.syxhen.addroidhome.ui.icons.close
import com.syxhen.addroidhome.ui.icons.search
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val FullDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryLogContent(
    state: QueryLogUiState,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onFilterSelected: (QueryLogFilter) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSetDomainBlocked: (QueryLogItemDto, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var selectedItem by remember { mutableStateOf<QueryLogItemDto?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search domain or client IP") },
            leadingIcon = {
                Icon(
                    imageVector = search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = close,
                            contentDescription = "Clear search",
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearchSubmit()
                },
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            items(QueryLogFilter.entries) { filter ->
                FilterChip(
                    selected = state.selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                state.items.isEmpty() && state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No DNS queries found",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (state.searchQuery.isNotBlank() || state.selectedFilter != QueryLogFilter.All) {
                                    "Try clearing your search or changing the status filter."
                                } else {
                                    "Pull down to refresh recent DNS activity."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { index, item -> "${item.time}_${item.displayDomain}_${item.client}_$index" },
                        ) { _, item ->
                            QueryLogRowCard(
                                item = item,
                                onClick = { selectedItem = item },
                            )
                        }

                        if (state.canLoadMore || state.isLoadingMore) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        OutlinedButton(onClick = onLoadMore) {
                                            Text("Load older queries")
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    val activeItem = selectedItem
    if (activeItem != null) {
        QueryLogDetailSheet(
            item = activeItem,
            isUpdatingRule = state.updatingDomain == activeItem.displayDomain,
            onDismiss = { selectedItem = null },
            onToggleBlock = { shouldBlock ->
                onSetDomainBlocked(activeItem, shouldBlock)
                selectedItem = null
            },
        )
    }
}

@Composable
private fun QueryLogRowCard(
    item: QueryLogItemDto,
    onClick: () -> Unit,
) {
    val badgeColors = statusColorsFor(item)
    val shortTime = remember(item.time) { formatShortTime(item.time) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.displayDomain.ifBlank { "(unknown domain)" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    color = badgeColors.first,
                    contentColor = badgeColors.second,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = item.reasonLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val clientSummary = buildString {
                    append(item.clientDisplayName.ifBlank { "Unknown client" })
                    if (item.question.type.isNotBlank()) {
                        append(" · ")
                        append(item.question.type)
                    }
                    if (item.cached) {
                        append(" · Cached")
                    }
                }
                Text(
                    text = clientSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                val timingText = buildString {
                    if (item.formattedElapsedMs.isNotBlank()) {
                        append(item.formattedElapsedMs)
                    }
                    if (shortTime.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(shortTime)
                    }
                }
                Text(
                    text = timingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueryLogDetailSheet(
    item: QueryLogItemDto,
    isUpdatingRule: Boolean,
    onDismiss: () -> Unit,
    onToggleBlock: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val badgeColors = statusColorsFor(item)
    val shouldBlock = !item.isBlocked

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayDomain.ifBlank { "(unknown domain)" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatFullTime(item.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = badgeColors.first,
                    contentColor = badgeColors.second,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = item.reasonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            if (item.displayDomain.isNotBlank()) {
                Button(
                    onClick = { onToggleBlock(shouldBlock) },
                    enabled = !isUpdatingRule,
                    colors = if (shouldBlock) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    if (isUpdatingRule) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (shouldBlock) block else check_circle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (shouldBlock) "Block domain" else "Unblock domain")
                    }
                }
            }

            HorizontalDivider()

            DetailRow(
                label = "Client",
                value = buildString {
                    append(item.clientDisplayName.ifBlank { "Unknown" })
                    if (item.client.isNotBlank() && item.client != item.clientDisplayName) {
                        append(" (${item.client})")
                    }
                },
            )
            DetailRow(
                label = "Query Type",
                value = listOf(item.question.type, item.question.dnsClass)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .ifBlank { "—" },
            )
            DetailRow(
                label = "Response Status",
                value = buildString {
                    append(item.status.ifBlank { "—" })
                    if (item.cached) append(" · Cached")
                    if (item.answerDnssec) append(" · DNSSEC")
                },
            )
            if (item.formattedElapsedMs.isNotBlank()) {
                DetailRow(label = "Elapsed Time", value = item.formattedElapsedMs)
            }
            if (item.upstream.isNotBlank()) {
                DetailRow(label = "Upstream", value = item.upstream)
            }
            if (item.clientProto.isNotBlank()) {
                DetailRow(label = "Protocol", value = item.clientProto.uppercase())
            }

            val appliedRules = item.effectiveRules
            if (appliedRules.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Matched Rules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                appliedRules.forEach { rule ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = rule.text.ifBlank { "(rule)" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = rule.sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (item.answer.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "DNS Answers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                item.answer.forEach { answer ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${answer.type}: ${answer.value}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TTL ${answer.ttl}s",
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

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun statusColorsFor(item: QueryLogItemDto): Pair<Color, Color> = when {
    item.isBlocked -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    item.isWhitelisted -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    item.isRewritten -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}

private fun formatShortTime(isoTimestamp: String): String = runCatching {
    OffsetDateTime.parse(isoTimestamp)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(TimeFormatter)
}.getOrDefault(isoTimestamp.take(19))

private fun formatFullTime(isoTimestamp: String): String = runCatching {
    OffsetDateTime.parse(isoTimestamp)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(FullDateTimeFormatter)
}.getOrDefault(isoTimestamp)

private fun sampleQueryLogItems(): List<QueryLogItemDto> = listOf(
    QueryLogItemDto(
        time = "2026-11-26T01:14:22+01:00",
        question = DnsQuestionDto(name = "ads.tracker.net", type = "A", dnsClass = "IN"),
        client = "192.168.1.15",
        clientInfo = QueryLogClientInfoDto(name = "Pixel 9"),
        elapsedMs = "0.42",
        reason = "FilteredBlackList",
        rules = listOf(ResultRuleDto(filterListId = 1L, text = "||ads.tracker.net^")),
        status = "NOERROR",
    ),
    QueryLogItemDto(
        time = "2026-11-26T01:14:19+01:00",
        question = DnsQuestionDto(name = "github.com", type = "AAAA", dnsClass = "IN"),
        answer = listOf(DnsAnswerDto(type = "AAAA", value = "2606:50c0:8000::154", ttl = 60)),
        cached = true,
        upstream = "https://dns.quad9.net/dns-query",
        client = "192.168.1.22",
        clientInfo = QueryLogClientInfoDto(name = "Workstation"),
        elapsedMs = "1.18",
        reason = "NotFilteredNotFound",
        status = "NOERROR",
    ),
    QueryLogItemDto(
        time = "2026-11-26T01:13:55+01:00",
        question = DnsQuestionDto(name = "analytics.allowed.example", type = "A", dnsClass = "IN"),
        client = "192.168.1.15",
        clientInfo = QueryLogClientInfoDto(name = "Pixel 9"),
        elapsedMs = "12.40",
        reason = "NotFilteredWhiteList",
        rules = listOf(ResultRuleDto(filterListId = 0L, text = "@@||analytics.allowed.example^")),
        upstream = "tls://1.1.1.1:853",
        status = "NOERROR",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Query Log Light", showBackground = true)
@Composable
private fun QueryLogScreenLightPreview() {
    AdDroidHomeTheme(dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Query Log") })
            },
        ) { innerPadding ->
            QueryLogContent(
                state = QueryLogUiState(
                    items = sampleQueryLogItems(),
                    selectedFilter = QueryLogFilter.All,
                    hasLoadedOnce = true,
                    canLoadMore = true,
                ),
                onSearchChange = {},
                onSearchSubmit = {},
                onFilterSelected = {},
                onRefresh = {},
                onLoadMore = {},
                onSetDomainBlocked = { _, _ -> },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Query Log Dark", showBackground = true)
@Composable
private fun QueryLogScreenDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Query Log") })
            },
        ) { innerPadding ->
            QueryLogContent(
                state = QueryLogUiState(
                    items = sampleQueryLogItems(),
                    searchQuery = "tracker",
                    selectedFilter = QueryLogFilter.Blocked,
                    hasLoadedOnce = true,
                ),
                onSearchChange = {},
                onSearchSubmit = {},
                onFilterSelected = {},
                onRefresh = {},
                onLoadMore = {},
                onSetDomainBlocked = { _, _ -> },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
