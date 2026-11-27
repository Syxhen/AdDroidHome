package com.syxhen.addroidhome.ui.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.data.network.model.RewriteEntryDto
import com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto
import com.syxhen.addroidhome.data.network.model.SafeSearchEngine
import com.syxhen.addroidhome.ui.SettingsUiState
import com.syxhen.addroidhome.ui.icons.add
import com.syxhen.addroidhome.ui.icons.delete
import com.syxhen.addroidhome.ui.icons.edit
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onRefresh: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    var showAddRewriteDialog by remember { mutableStateOf(false) }
    var editingRewrite by remember { mutableStateOf<RewriteEntryDto?>(null) }
    var deletingRewrite by remember { mutableStateOf<RewriteEntryDto?>(null) }
    var showClearLogConfirm by remember { mutableStateOf(false) }
    var showResetStatsConfirm by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.hasLoadedOnce,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (!state.hasLoadedOnce && state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "protection_toggles") {
                    ProtectionSettingsCard(
                        state = state,
                        onSetSafeBrowsingEnabled = onSetSafeBrowsingEnabled,
                        onSetParentalEnabled = onSetParentalEnabled,
                        onSetSafeSearchEnabled = onSetSafeSearchEnabled,
                        onToggleSafeSearchEngine = onToggleSafeSearchEngine,
                    )
                }

                item(key = "dns_rewrites") {
                    DnsRewritesCard(
                        state = state,
                        onSetRewritesEnabled = onSetRewritesEnabled,
                        onAddClick = { showAddRewriteDialog = true },
                        onToggleEntry = { entry, enabled ->
                            onUpdateRewrite(entry, entry.copy(enabled = enabled))
                        },
                        onEditEntry = { editingRewrite = it },
                        onDeleteEntry = { deletingRewrite = it },
                    )
                }

                item(key = "maintenance") {
                    MaintenanceCard(
                        isClearingQueryLog = state.isClearingQueryLog,
                        isResettingStats = state.isResettingStats,
                        onClearQueryLogClick = { showClearLogConfirm = true },
                        onResetStatsClick = { showResetStatsConfirm = true },
                    )
                }
            }
        }
    }

    if (showAddRewriteDialog) {
        RewriteEditorDialog(
            title = "Add DNS Rewrite",
            confirmLabel = "Add",
            initialDomain = "",
            initialAnswer = "",
            initialEnabled = true,
            isSaving = state.isMutatingRewrite,
            onDismiss = { showAddRewriteDialog = false },
            onConfirm = { domain, answer, enabled ->
                onAddRewrite(domain, answer, enabled)
                showAddRewriteDialog = false
            },
        )
    }

    editingRewrite?.let { target ->
        RewriteEditorDialog(
            title = "Edit DNS Rewrite",
            confirmLabel = "Save",
            initialDomain = target.domain,
            initialAnswer = target.answer,
            initialEnabled = target.enabled,
            isSaving = state.isMutatingRewrite,
            onDismiss = { editingRewrite = null },
            onConfirm = { domain, answer, enabled ->
                onUpdateRewrite(
                    target,
                    RewriteEntryDto(
                        domain = domain,
                        answer = answer,
                        enabled = enabled,
                    ),
                )
                editingRewrite = null
            },
        )
    }

    deletingRewrite?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingRewrite = null },
            title = { Text("Delete DNS Rewrite?") },
            text = {
                Text("Remove rewrite mapping ${entry.domain} -> ${entry.answer}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRewrite(entry)
                        deletingRewrite = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRewrite = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClearLogConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogConfirm = false },
            title = { Text("Clear Query Log?") },
            text = {
                Text(
                    "This permanently deletes all stored DNS query log entries on your AdGuard Home server.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearLogConfirm = false
                        onClearQueryLog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Clear Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showResetStatsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetStatsConfirm = false },
            title = { Text("Reset Statistics?") },
            text = {
                Text(
                    "This resets all AdGuard Home query counters, charts, and top lists back to zero.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStatsConfirm = false
                        onResetStats()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Reset Stats")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProtectionSettingsCard(
    state: SettingsUiState,
    onSetSafeBrowsingEnabled: (Boolean) -> Unit,
    onSetParentalEnabled: (Boolean) -> Unit,
    onSetSafeSearchEnabled: (Boolean) -> Unit,
    onToggleSafeSearchEngine: (SafeSearchEngine, Boolean) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Protection & Content Filtering",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SettingToggleRow(
                title = "Safe Browsing",
                subtitle = "Block phishing, malware, and fraudulent domains",
                checked = state.safeBrowsingEnabled,
                enabled = !state.isUpdatingSafeBrowsing && !state.isLoading,
                switchDescription = "Toggle Safe Browsing",
                onCheckedChange = onSetSafeBrowsingEnabled,
            )

            HorizontalDivider()

            SettingToggleRow(
                title = "Parental Control",
                subtitle = "Block access to adult content domains",
                checked = state.parentalEnabled,
                enabled = !state.isUpdatingParental && !state.isLoading,
                switchDescription = "Toggle Parental Control",
                onCheckedChange = onSetParentalEnabled,
            )

            HorizontalDivider()

            val safeSearch = state.safeSearch
            val safeSearchSubtitle = if (safeSearch.enabled) {
                "Enforcing safe results on ${safeSearch.enabledEngineCount}/${SafeSearchEngine.entries.size} engines"
            } else {
                "Enforce safe search on supported search engines"
            }

            SettingToggleRow(
                title = "Safe Search",
                subtitle = safeSearchSubtitle,
                checked = safeSearch.enabled,
                enabled = !state.isUpdatingSafeSearch && !state.isLoading,
                switchDescription = "Toggle Safe Search",
                onCheckedChange = onSetSafeSearchEnabled,
            )

            if (safeSearch.enabled) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SafeSearchEngine.entries.forEach { engine ->
                        val isEngineOn = safeSearch.isEngineEnabled(engine)
                        FilterChip(
                            selected = isEngineOn,
                            onClick = { onToggleSafeSearchEngine(engine, !isEngineOn) },
                            enabled = !state.isUpdatingSafeSearch && !state.isLoading,
                            label = { Text(engine.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    switchDescription: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = switchDescription
            },
        )
    }
}

@Composable
private fun DnsRewritesCard(
    state: SettingsUiState,
    onSetRewritesEnabled: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onToggleEntry: (RewriteEntryDto, Boolean) -> Unit,
    onEditEntry: (RewriteEntryDto) -> Unit,
    onDeleteEntry: (RewriteEntryDto) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DNS Rewrites",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when (state.rewrites.size) {
                            0 -> "Map domains to custom IPs or CNAMEs"
                            1 -> "1 custom DNS mapping"
                            else -> "${state.rewrites.size} custom DNS mappings"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalButton(
                    onClick = onAddClick,
                    enabled = !state.isMutatingRewrite && !state.isLoading,
                ) {
                    Icon(
                        imageVector = add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }

            state.rewritesEnabled?.let { globallyEnabled ->
                HorizontalDivider()
                SettingToggleRow(
                    title = "Enable DNS Rewrites",
                    subtitle = "Apply configured rewrite rules to incoming DNS queries",
                    checked = globallyEnabled,
                    enabled = !state.isUpdatingRewritesEnabled && !state.isLoading,
                    switchDescription = "Toggle DNS rewrites globally",
                    onCheckedChange = onSetRewritesEnabled,
                )
            }

            if (state.rewrites.isEmpty()) {
                Text(
                    text = "No DNS rewrites configured yet. Tap Add to map a local domain or wildcard to an IP or hostname.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.rewrites.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        RewriteEntryRow(
                            entry = entry,
                            enabled = !state.isMutatingRewrite && !state.isLoading,
                            onToggleEnabled = { newEnabled -> onToggleEntry(entry, newEnabled) },
                            onEdit = { onEditEntry(entry) },
                            onDelete = { onDeleteEntry(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RewriteEntryRow(
    entry: RewriteEntryDto,
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = entry.domain,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "-> ${entry.answer}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Switch(
                checked = entry.enabled,
                onCheckedChange = onToggleEnabled,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = "Toggle rewrite for ${entry.domain}"
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (entry.enabled) "Active" else "Disabled",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = edit,
                        contentDescription = "Edit rewrite for ${entry.domain}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = delete,
                        contentDescription = "Delete rewrite for ${entry.domain}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceCard(
    isClearingQueryLog: Boolean,
    isResettingStats: Boolean,
    onClearQueryLogClick: () -> Unit,
    onResetStatsClick: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Server Maintenance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Clear stored logs or reset dashboard counters on AdGuard Home.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onClearQueryLogClick,
                enabled = !isClearingQueryLog,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(),
            ) {
                Text(if (isClearingQueryLog) "Clearing Query Log..." else "Clear Query Log")
            }

            OutlinedButton(
                onClick = onResetStatsClick,
                enabled = !isResettingStats,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(),
            ) {
                Text(if (isResettingStats) "Resetting Statistics..." else "Reset Statistics")
            }
        }
    }
}

@Composable
private fun RewriteEditorDialog(
    title: String,
    confirmLabel: String,
    initialDomain: String,
    initialAnswer: String,
    initialEnabled: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (domain: String, answer: String, enabled: Boolean) -> Unit,
) {
    var domain by remember(initialDomain) { mutableStateOf(initialDomain) }
    var answer by remember(initialAnswer) { mutableStateOf(initialAnswer) }
    var enabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }

    val canSave = domain.isNotBlank() && answer.isNotBlank() && !isSaving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain or wildcard") },
                    placeholder = { Text("home.lan or *.home.lan") },
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(max = 488.dp)
                        .fillMaxWidth(),
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("IP address or target domain") },
                    placeholder = { Text("192.168.1.10, server.local, A, AAAA") },
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(max = 488.dp)
                        .fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        modifier = Modifier.semantics {
                            contentDescription = "Enable rewrite rule"
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(domain.trim(), answer.trim(), enabled) },
                enabled = canSave,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

internal fun sampleSettingsState(): SettingsUiState = SettingsUiState(
    safeBrowsingEnabled = true,
    parentalEnabled = false,
    safeSearch = SafeSearchConfigDto(
        enabled = true,
        google = true,
        bing = true,
        youTube = true,
        duckDuckGo = true,
        ecosia = false,
        pixabay = false,
        yandex = true,
    ),
    rewritesEnabled = true,
    rewrites = listOf(
        RewriteEntryDto(domain = "nas.home.lan", answer = "192.168.1.50", enabled = true),
        RewriteEntryDto(domain = "*.lab.internal", answer = "192.168.1.10", enabled = true),
        RewriteEntryDto(domain = "old.printer.lan", answer = "192.168.1.88", enabled = false),
    ),
    hasLoadedOnce = true,
)

@Preview(name = "Settings Light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    AdDroidHomeTheme(dynamicColor = false) {
        Surface {
            SettingsContent(
                state = sampleSettingsState(),
                onRefresh = {},
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
            )
        }
    }
}

@Preview(name = "Settings Dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SettingsContent(
                state = sampleSettingsState(),
                onRefresh = {},
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
            )
        }
    }
}
