package com.syxhen.addroidhome.ui.services

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.data.network.model.BlockedServiceDto
import com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto
import com.syxhen.addroidhome.data.network.model.ServiceGroupDto
import com.syxhen.addroidhome.data.network.model.formatServiceGroupLabel
import com.syxhen.addroidhome.ui.BlockedServicesUiState
import com.syxhen.addroidhome.ui.icons.apps
import com.syxhen.addroidhome.ui.icons.close
import com.syxhen.addroidhome.ui.icons.save
import com.syxhen.addroidhome.ui.icons.search
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedServicesContent(
    state: BlockedServicesUiState,
    onSearchChange: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onShowBlockedOnlyChange: (Boolean) -> Unit,
    onToggleService: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDiscardChanges: () -> Unit,
    onSaveChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val availableGroups = remember(state.groups, state.services) {
        if (state.groups.isNotEmpty()) {
            state.groups
        } else {
            state.services
                .map { it.groupId }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .map { ServiceGroupDto(id = it) }
        }
    }
    val filteredServices = remember(
        state.services,
        state.selectedIds,
        state.searchQuery,
        state.selectedGroupId,
        state.showBlockedOnly,
    ) {
        state.filteredServices
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search services (e.g. YouTube, TikTok)") },
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
                    onSearch = { focusManager.clearFocus() },
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .widthIn(max = 488.dp)
                    .fillMaxWidth(),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "blocked_only_chip") {
                FilterChip(
                    selected = state.showBlockedOnly,
                    onClick = { onShowBlockedOnlyChange(!state.showBlockedOnly) },
                    label = { Text("Blocked (${state.blockedCount})") },
                )
            }

            item(key = "all_groups_chip") {
                FilterChip(
                    selected = state.selectedGroupId == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text("All") },
                )
            }

            items(
                items = availableGroups,
                key = { "group_${it.id}" },
            ) { group ->
                FilterChip(
                    selected = state.selectedGroupId == group.id,
                    onClick = {
                        val next = if (state.selectedGroupId == group.id) null else group.id
                        onGroupSelected(next)
                    },
                    label = { Text(group.displayLabel) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.services.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.services.isEmpty() && state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading blockable services...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (state.services.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = apps,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Unable to load services",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "services_summary") {
                        ServicesSummarySection(
                            totalServices = state.services.size,
                            blockedCount = state.blockedCount,
                            visibleCount = filteredServices.size,
                            schedule = state.currentSchedule,
                        )
                    }

                    if (filteredServices.isEmpty()) {
                        item(key = "empty_filtered") {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "No matching services",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Try clearing the search query or switching category filters.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredServices,
                            key = { it.id },
                        ) { service ->
                            val isBlocked = service.id in state.selectedIds
                            val isChanged = isBlocked != (service.id in state.savedIds)
                            BlockedServiceCard(
                                service = service,
                                isBlocked = isBlocked,
                                isChanged = isChanged,
                                enabled = !state.isSaving,
                                onToggle = { checked -> onToggleService(service.id, checked) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.hasUnsavedChanges) {
            UnsavedServicesBar(
                pendingChangeCount = state.pendingChangeCount,
                isSaving = state.isSaving,
                onDiscard = onDiscardChanges,
                onSave = onSaveChanges,
            )
        }
    }
}

@Composable
private fun ServicesSummarySection(
    totalServices: Int,
    blockedCount: Int,
    visibleCount: Int,
    schedule: BlockedServicesScheduleDto?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = buildString {
                append("$blockedCount of $totalServices services blocked")
                if (visibleCount != totalServices) {
                    append(" · Showing $visibleCount")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (schedule?.hasActiveSchedule == true) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = com.syxhen.addroidhome.ui.icons.schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = buildString {
                            val days = schedule.activeScheduleDaysCount
                            append("Pause schedule active ($days ")
                            append(if (days == 1) "day" else "days")
                            schedule.scheduleTimeZone?.let { append(" · $it") }
                            append(") — preserved on save")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedServiceCard(
    service: BlockedServiceDto,
    isBlocked: Boolean,
    isChanged: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = if (isBlocked) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = serviceMonogram(service.displayName),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = service.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val subtitle = buildString {
                        if (service.groupId.isNotBlank()) {
                            append(formatServiceGroupLabel(service.groupId))
                        }
                        if (service.rules.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("${service.rules.size} rules")
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isChanged) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "Unsaved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = "Block ${service.displayName}"
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnsavedServicesBar(
    pendingChangeCount: Int,
    isSaving: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = if (pendingChangeCount == 1) {
                    "1 unsaved change"
                } else {
                    "$pendingChangeCount unsaved changes"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                ) {
                    Text("Discard")
                }

                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

private fun serviceMonogram(name: String): String {
    val words = name.trim().split(Regex("""[\s._-]+""")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase(Locale.ROOT)
        else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.ROOT)
    }
}

private val PreviewServices = listOf(
    BlockedServiceDto(
        id = "chatgpt",
        name = "ChatGPT",
        groupId = "ai",
        rules = listOf("||chatgpt.com^", "||openai.com^"),
    ),
    BlockedServiceDto(
        id = "discord",
        name = "Discord",
        groupId = "messenger",
        rules = listOf("||discord.com^", "||discord.gg^", "||discordapp.com^"),
    ),
    BlockedServiceDto(
        id = "instagram",
        name = "Instagram",
        groupId = "social_network",
        rules = listOf("||instagram.com^", "||cdninstagram.com^"),
    ),
    BlockedServiceDto(
        id = "steam",
        name = "Steam",
        groupId = "gaming",
        rules = listOf("||steampowered.com^", "||steamcommunity.com^"),
    ),
    BlockedServiceDto(
        id = "tiktok",
        name = "TikTok",
        groupId = "social_network",
        rules = listOf("||tiktok.com^", "||tiktokcdn.com^"),
    ),
    BlockedServiceDto(
        id = "youtube",
        name = "YouTube",
        groupId = "streaming",
        rules = listOf("||youtube.com^", "||youtu.be^", "||googlevideo.com^"),
    ),
)

private val PreviewGroups = listOf(
    ServiceGroupDto("social_network"),
    ServiceGroupDto("streaming"),
    ServiceGroupDto("gaming"),
    ServiceGroupDto("messenger"),
    ServiceGroupDto("ai"),
)

private val PreviewSchedule = BlockedServicesScheduleDto(
    ids = listOf("tiktok"),
    schedule = JsonObject(
        mapOf(
            "time_zone" to JsonPrimitive("Europe/Rome"),
            "mon" to JsonObject(mapOf("start" to JsonPrimitive(32_400_000), "end" to JsonPrimitive(64_800_000))),
            "tue" to JsonObject(mapOf("start" to JsonPrimitive(32_400_000), "end" to JsonPrimitive(64_800_000))),
            "wed" to JsonObject(mapOf("start" to JsonPrimitive(32_400_000), "end" to JsonPrimitive(64_800_000))),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun BlockedServicesLightPreview() {
    AdDroidHomeTheme(dynamicColor = false, darkTheme = false) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Blocked Services") }) },
        ) { innerPadding ->
            BlockedServicesContent(
                state = BlockedServicesUiState(
                    services = PreviewServices,
                    groups = PreviewGroups,
                    savedIds = setOf("tiktok"),
                    selectedIds = setOf("tiktok", "instagram"),
                    currentSchedule = PreviewSchedule,
                    hasLoadedOnce = true,
                ),
                onSearchChange = {},
                onGroupSelected = {},
                onShowBlockedOnlyChange = {},
                onToggleService = { _, _ -> },
                onRefresh = {},
                onDiscardChanges = {},
                onSaveChanges = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun BlockedServicesDarkPreview() {
    AdDroidHomeTheme(dynamicColor = false, darkTheme = true) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Blocked Services") }) },
        ) { innerPadding ->
            BlockedServicesContent(
                state = BlockedServicesUiState(
                    services = PreviewServices,
                    groups = PreviewGroups,
                    savedIds = setOf("tiktok", "youtube"),
                    selectedIds = setOf("tiktok", "youtube"),
                    currentSchedule = BlockedServicesScheduleDto(ids = listOf("tiktok", "youtube")),
                    showBlockedOnly = true,
                    hasLoadedOnce = true,
                ),
                onSearchChange = {},
                onGroupSelected = {},
                onShowBlockedOnlyChange = {},
                onToggleService = { _, _ -> },
                onRefresh = {},
                onDiscardChanges = {},
                onSaveChanges = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
