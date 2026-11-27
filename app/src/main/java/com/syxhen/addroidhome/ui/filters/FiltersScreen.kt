package com.syxhen.addroidhome.ui.filters

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.data.network.model.FilterListDto
import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.ui.FiltersSection
import com.syxhen.addroidhome.ui.FiltersUiState
import com.syxhen.addroidhome.ui.icons.add
import com.syxhen.addroidhome.ui.icons.delete
import com.syxhen.addroidhome.ui.icons.filter_list
import com.syxhen.addroidhome.ui.icons.refresh
import com.syxhen.addroidhome.ui.icons.save
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val LAST_UPDATED_PATTERN = "MMM d, HH:mm"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersContent(
    state: FiltersUiState,
    onSelectSection: (FiltersSection) -> Unit,
    onRefreshStatus: () -> Unit,
    onSetFilteringEnabled: (Boolean) -> Unit,
    onToggleFilterList: (FilterListDto, Boolean, Boolean) -> Unit,
    onAddFilterList: (name: String, url: String, whitelist: Boolean) -> Unit,
    onRemoveFilterList: (FilterListDto, Boolean) -> Unit,
    onCheckForUpdates: (whitelist: Boolean) -> Unit,
    onCustomRulesDraftChange: (String) -> Unit,
    onDiscardCustomRules: () -> Unit,
    onSaveCustomRules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addDialogWhitelist by remember { mutableStateOf<Boolean?>(null) }
    var pendingDeleteTarget by remember { mutableStateOf<Pair<FilterListDto, Boolean>?>(null) }

    val status = state.status

    PullToRefreshBox(
        isRefreshing = state.isLoading && status != null,
        onRefresh = onRefreshStatus,
        modifier = modifier.fillMaxSize(),
    ) {
        if (status == null && state.isLoading) {
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
                        text = "Loading DNS filters...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (status == null) {
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
                        imageVector = filter_list,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Unable to load filters",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(onClick = onRefreshStatus) {
                        Text("Retry")
                    }
                }
            }
        } else {
            val isAllowlistSection = state.selectedSection == FiltersSection.Allowlists
            val activeLists = if (isAllowlistSection) status.whitelistFilters else status.filters

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "overview") {
                    FilteringOverviewCard(
                        status = status,
                        activeCustomRulesCount = state.activeCustomRulesCount,
                        isUpdatingEnabled = state.isUpdatingFilteringEnabled,
                        onToggleEnabled = onSetFilteringEnabled,
                    )
                }

                item(key = "section_selector") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FiltersSection.entries.forEach { section ->
                            val label = when (section) {
                                FiltersSection.Blocklists -> "Blocklists (${status.filters.size})"
                                FiltersSection.Allowlists -> "Allowlists (${status.whitelistFilters.size})"
                                FiltersSection.CustomRules -> "Custom Rules (${state.activeCustomRulesCount})"
                            }
                            FilterChip(
                                selected = state.selectedSection == section,
                                onClick = { onSelectSection(section) },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                when (state.selectedSection) {
                    FiltersSection.Blocklists,
                    FiltersSection.Allowlists,
                    -> {
                        item(key = "list_actions_${state.selectedSection.name}") {
                            FilterListSectionActions(
                                isAllowlist = isAllowlistSection,
                                enabledCount = if (isAllowlistSection) {
                                    status.enabledAllowlistsCount
                                } else {
                                    status.enabledBlocklistsCount
                                },
                                totalCount = activeLists.size,
                                activeRulesCount = if (isAllowlistSection) {
                                    status.activeAllowlistRulesCount
                                } else {
                                    status.activeBlocklistRulesCount
                                },
                                isRefreshingLists = state.isRefreshingLists,
                                isAddingFilter = state.isAddingFilter,
                                onCheckForUpdates = { onCheckForUpdates(isAllowlistSection) },
                                onAddClick = { addDialogWhitelist = isAllowlistSection },
                            )
                        }

                        if (activeLists.isEmpty()) {
                            item(key = "empty_${state.selectedSection.name}") {
                                EmptyFilterListsCard(
                                    isAllowlist = isAllowlistSection,
                                    isAddingFilter = state.isAddingFilter,
                                    onAddClick = { addDialogWhitelist = isAllowlistSection },
                                )
                            }
                        } else {
                            items(
                                items = activeLists,
                                key = { "${state.selectedSection.name}_${it.id}_${it.url}" },
                            ) { filter ->
                                FilterListCard(
                                    filter = filter,
                                    isAllowlist = isAllowlistSection,
                                    isToggling = state.togglingFilterUrl == filter.url,
                                    isDeleting = state.deletingFilterUrl == filter.url,
                                    isBusyGlobally = state.togglingFilterUrl != null ||
                                        state.deletingFilterUrl != null,
                                    onToggle = { enabled ->
                                        onToggleFilterList(filter, isAllowlistSection, enabled)
                                    },
                                    onDeleteClick = {
                                        pendingDeleteTarget = filter to isAllowlistSection
                                    },
                                )
                            }
                        }
                    }

                    FiltersSection.CustomRules -> {
                        item(key = "custom_rules_editor") {
                            CustomRulesEditorCard(
                                draft = state.customRulesDraft,
                                activeRulesCount = state.activeCustomRulesCount,
                                hasUnsavedChanges = state.hasUnsavedCustomRules,
                                isSaving = state.isSavingCustomRules,
                                onDraftChange = onCustomRulesDraftChange,
                                onDiscard = onDiscardCustomRules,
                                onSave = onSaveCustomRules,
                            )
                        }
                    }
                }
            }
        }
    }

    addDialogWhitelist?.let { isWhitelist ->
        AddFilterListDialog(
            whitelist = isWhitelist,
            onDismiss = { addDialogWhitelist = null },
            onConfirm = { name, url ->
                addDialogWhitelist = null
                onAddFilterList(name, url, isWhitelist)
            },
        )
    }

    pendingDeleteTarget?.let { (filter, isWhitelist) ->
        RemoveFilterListDialog(
            filter = filter,
            whitelist = isWhitelist,
            onDismiss = { pendingDeleteTarget = null },
            onConfirm = {
                pendingDeleteTarget = null
                onRemoveFilterList(filter, isWhitelist)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilteringOverviewCard(
    status: FilterStatusDto,
    activeCustomRulesCount: Int,
    isUpdatingEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "DNS Filtering",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = buildString {
                            append(if (status.enabled) "Active" else "Disabled")
                            if (status.intervalHours > 0) {
                                append(" · Auto-updates every ${formatIntervalHours(status.intervalHours)}")
                            } else {
                                append(" · Auto-updates disabled")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = status.enabled,
                    onCheckedChange = onToggleEnabled,
                    enabled = !isUpdatingEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle global DNS filtering"
                    },
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetadataPill(
                    text = "${formatCount(status.activeBlocklistRulesCount)} block rules",
                )
                MetadataPill(
                    text = "${formatCount(status.activeAllowlistRulesCount)} allow rules",
                )
                MetadataPill(
                    text = "$activeCustomRulesCount custom",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterListSectionActions(
    isAllowlist: Boolean,
    enabledCount: Int,
    totalCount: Int,
    activeRulesCount: Long,
    isRefreshingLists: Boolean,
    isAddingFilter: Boolean,
    onCheckForUpdates: () -> Unit,
    onAddClick: () -> Unit,
) {
    val kindPlural = if (isAllowlist) "allowlists" else "blocklists"
    val addLabel = if (isAllowlist) "Add allowlist" else "Add blocklist"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$enabledCount of $totalCount $kindPlural enabled · ${formatCount(activeRulesCount)} active rules",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !isRefreshingLists && totalCount > 0,
            ) {
                if (isRefreshingLists) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isRefreshingLists) "Updating..." else "Check updates")
            }

            FilledTonalButton(
                onClick = onAddClick,
                enabled = !isAddingFilter,
            ) {
                if (isAddingFilter) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAddingFilter) "Adding..." else addLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterListCard(
    filter: FilterListDto,
    isAllowlist: Boolean,
    isToggling: Boolean,
    isDeleting: Boolean,
    isBusyGlobally: Boolean,
    onToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = filter.name.ifBlank { "Unnamed ${if (isAllowlist) "allowlist" else "blocklist"}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = filter.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isToggling || isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Switch(
                            checked = filter.enabled,
                            onCheckedChange = onToggle,
                            enabled = !isBusyGlobally,
                            modifier = Modifier.semantics {
                                contentDescription = "Toggle ${filter.name.ifBlank { filter.url }}"
                            },
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        enabled = !isBusyGlobally,
                    ) {
                        Icon(
                            imageVector = delete,
                            contentDescription = "Remove ${filter.name.ifBlank { "filter list" }}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetadataPill(text = "${formatCount(filter.rulesCount)} rules")

                val formattedUpdate = formatLastUpdated(filter.lastUpdated)
                if (formattedUpdate != null) {
                    MetadataPill(text = "Updated $formattedUpdate")
                }

                if (!filter.enabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterListsCard(
    isAllowlist: Boolean,
    isAddingFilter: Boolean,
    onAddClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = filter_list,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isAllowlist) "No DNS allowlists configured" else "No DNS blocklists configured",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isAllowlist) {
                    "Add a hosted allowlist URL to exempt trusted domains from blocking."
                } else {
                    "Add a hosted blocklist URL to start filtering ads and trackers."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onAddClick,
                enabled = !isAddingFilter,
            ) {
                Icon(
                    imageVector = add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAllowlist) "Add allowlist" else "Add blocklist")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomRulesEditorCard(
    draft: String,
    activeRulesCount: Int,
    hasUnsavedChanges: Boolean,
    isSaving: Boolean,
    onDraftChange: (String) -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Custom Filtering Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$activeRulesCount active rules saved on server",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (hasUnsavedChanges) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Unsaved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "||example.org^  Block domain & subdomains",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "@@||example.org^  Allow domain & subdomains",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "! Comment line",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .widthIn(max = 488.dp)
                    .fillMaxWidth(),
                enabled = !isSaving,
                label = { Text("Rules (one per line)") },
                placeholder = {
                    Text(
                        text = "||ads.example.com^\n@@||trusted.example.com^",
                        fontFamily = FontFamily.Monospace,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                minLines = 8,
                maxLines = 16,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = hasUnsavedChanges && !isSaving,
                ) {
                    Text("Discard")
                }

                Button(
                    onClick = onSave,
                    enabled = hasUnsavedChanges && !isSaving,
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
                    Text(if (isSaving) "Saving..." else "Save rules")
                }
            }
        }
    }
}

@Composable
private fun AddFilterListDialog(
    whitelist: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val kind = if (whitelist) "Allowlist" else "Blocklist"
    val trimmedName = name.trim()
    val trimmedUrl = url.trim()
    val isUrlValid = trimmedUrl.startsWith("https://", ignoreCase = true) ||
        trimmedUrl.startsWith("http://", ignoreCase = true) ||
        trimmedUrl.startsWith("/")
    val canSubmit = trimmedName.isNotEmpty() && isUrlValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add DNS $kind") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. OISD Big") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("List URL or server file path") },
                    placeholder = { Text("https://example.com/list.txt") },
                    singleLine = true,
                    isError = trimmedUrl.isNotEmpty() && !isUrlValid,
                    supportingText = if (trimmedUrl.isNotEmpty() && !isUrlValid) {
                        { Text("Must start with https://, http://, or /") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName, trimmedUrl) },
                enabled = canSubmit,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun RemoveFilterListDialog(
    filter: FilterListDto,
    whitelist: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val kind = if (whitelist) "allowlist" else "blocklist"
    val displayName = filter.name.ifBlank { filter.url }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove $kind?") },
        text = {
            Text("Remove \"$displayName\" from AdGuard Home? You can add it again later.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MetadataPill(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun formatCount(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

private fun formatIntervalHours(hours: Int): String = when {
    hours <= 0 -> "disabled"
    hours == 1 -> "1 hour"
    hours < 24 -> "$hours hours"
    hours == 24 -> "24 hours"
    hours % 24 == 0 -> "${hours / 24} days"
    else -> "$hours hours"
}

private fun formatLastUpdated(raw: String): String? {
    if (raw.isBlank()) return null
    return runCatching {
        val formatter = DateTimeFormatter.ofPattern(LAST_UPDATED_PATTERN, Locale.getDefault())
        OffsetDateTime.parse(raw)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(formatter)
    }.getOrNull()
}

private val PreviewFilterStatus = FilterStatusDto(
    enabled = true,
    intervalHours = 24,
    filters = listOf(
        FilterListDto(
            id = 1L,
            enabled = true,
            url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
            name = "AdGuard DNS filter",
            rulesCount = 58_420L,
            lastUpdated = "2026-11-26T00:15:00+01:00",
        ),
        FilterListDto(
            id = 2L,
            enabled = true,
            url = "https://big.oisd.nl",
            name = "OISD Blocklist Big",
            rulesCount = 246_100L,
            lastUpdated = "2026-11-25T18:40:00+01:00",
        ),
        FilterListDto(
            id = 3L,
            enabled = false,
            url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_2.txt",
            name = "AdAway Default Blocklist",
            rulesCount = 6_810L,
            lastUpdated = "2026-11-24T12:00:00+01:00",
        ),
    ),
    whitelistFilters = listOf(
        FilterListDto(
            id = 10L,
            enabled = true,
            url = "https://example.com/allowlist.txt",
            name = "Home Services Allowlist",
            rulesCount = 42L,
            lastUpdated = "2026-11-25T21:10:00+01:00",
        ),
    ),
    userRules = listOf(
        "! Block noisy telemetry",
        "||metrics.example.net^",
        "@@||api.trusted-service.com^",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FiltersScreenLightPreview() {
    AdDroidHomeTheme(dynamicColor = false, darkTheme = false) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Filters") }) },
        ) { innerPadding ->
            FiltersContent(
                state = FiltersUiState(
                    status = PreviewFilterStatus,
                    selectedSection = FiltersSection.Blocklists,
                    hasLoadedOnce = true,
                    customRulesDraft = PreviewFilterStatus.userRules.joinToString("\n"),
                ),
                onSelectSection = {},
                onRefreshStatus = {},
                onSetFilteringEnabled = {},
                onToggleFilterList = { _, _, _ -> },
                onAddFilterList = { _, _, _ -> },
                onRemoveFilterList = { _, _ -> },
                onCheckForUpdates = {},
                onCustomRulesDraftChange = {},
                onDiscardCustomRules = {},
                onSaveCustomRules = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FiltersCustomRulesDarkPreview() {
    AdDroidHomeTheme(dynamicColor = false, darkTheme = true) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Filters") }) },
        ) { innerPadding ->
            FiltersContent(
                state = FiltersUiState(
                    status = PreviewFilterStatus,
                    selectedSection = FiltersSection.CustomRules,
                    hasLoadedOnce = true,
                    customRulesDraft = (PreviewFilterStatus.userRules + "||ads.tracker.io^").joinToString("\n"),
                ),
                onSelectSection = {},
                onRefreshStatus = {},
                onSetFilteringEnabled = {},
                onToggleFilterList = { _, _, _ -> },
                onAddFilterList = { _, _, _ -> },
                onRemoveFilterList = { _, _ -> },
                onCheckForUpdates = {},
                onCustomRulesDraftChange = {},
                onDiscardCustomRules = {},
                onSaveCustomRules = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
