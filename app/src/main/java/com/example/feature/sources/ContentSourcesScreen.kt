package com.example.feature.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.database.ContentSourceEntity
import com.example.core.database.SyncLogEntity
import com.example.core.model.AppStrings
import com.example.core.model.ConnectionValidationResult
import com.example.core.model.SourceType
import com.example.datasource.connectors.SourceConnectorRegistry
import com.example.ui.theme.KrasAmber
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasError
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasTeal
import com.example.ui.theme.KrasWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContentSourcesScreen(
    sources: List<ContentSourceEntity>,
    syncLogs: List<SyncLogEntity>,
    isSyncingAll: Boolean,
    lastStatusBanner: String?,
    validationFeedback: Map<Long, ConnectionValidationResult>,
    autoPlayNext: Boolean,
    strings: AppStrings,
    onAddSource: (ContentSourceEntity) -> Unit,
    onUpdateSource: (ContentSourceEntity) -> Unit,
    onToggleSourceEnabled: (ContentSourceEntity, Boolean) -> Unit,
    onTestConnection: (ContentSourceEntity) -> Unit,
    onSyncSource: (Long) -> Unit,
    onSyncAll: () -> Unit,
    onRemoveSource: (ContentSourceEntity) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleAutoPlayNext: (Boolean) -> Unit
) {
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<ContentSourceEntity?>(null) }
    var sourcePendingDeletion by remember { mutableStateOf<ContentSourceEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
            .statusBarsPadding()
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.contentSourcesTitle,
                    style = MaterialTheme.typography.displaySmall,
                    color = KrasWhite
                )
                Text(
                    text = if (strings.isArabic) {
                        "إدارة ومزامنة المصادر المصرحة وواجهات API"
                    } else {
                        "Manage & synchronize authorized APIs, JSON, XML/RSS & M3U sources"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = KrasSilver
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSyncAll,
                    enabled = !isSyncingAll && sources.any { it.isEnabled },
                    border = BorderStroke(1.dp, KrasCardBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("sync_all_sources_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = KrasTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.syncAll, color = KrasWhite)
                }

                Button(
                    onClick = {
                        editingSource = null
                        showAddOrEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KrasRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_source_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addSource)
                }
            }
        }

        if (isSyncingAll) {
            LinearProgressIndicator(
                color = KrasRed,
                trackColor = KrasCharcoal,
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("sources_lazy_column")
        ) {
            // Security & Permissions Notice Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = KrasObsidian),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = KrasEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "الأمان وتراخيص المحتوى (HTTPS افتراضياً)" else "HTTPS Enforced • Authorized Sources Only",
                                style = MaterialTheme.typography.titleSmall,
                                color = KrasWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isArabic) {
                                    "لا يتجاوز التطبيق أنظمة DRM أو الحماية أو جدران الدفع. يتم الحفاظ على الكتالوج المحلي في حال انقطاع اتصال أحد المصادر مؤقتاً."
                                } else {
                                    "Kras Movies respects DRM, authentication, and robots.txt rules. Offline catalog metadata is preserved if a source is temporarily unreachable."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = KrasSilver
                            )
                        }
                    }
                }
            }

            // Status Feedback Banner
            if (!lastStatusBanner.isNullOrBlank()) {
                item {
                    Surface(
                        color = KrasCharcoal,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, KrasTeal.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = lastStatusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KrasWhite,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // App Preferences Row (Language RTL/LTR + Auto-Play Next Episode)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = KrasGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (strings.isArabic) "لغة الواجهة والاتجاه (RTL / LTR)" else "Interface Language & Layout (LTR / RTL)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrasWhite
                                )
                                Text(
                                    text = if (strings.isArabic) "العربية (من اليمين لليسار)" else "English (Left-to-Right)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrasSilver
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onToggleLanguage,
                            border = BorderStroke(1.dp, KrasRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.languageToggleLabel, color = KrasWhite)
                        }
                    }
                }
            }

            // Source Cards
            items(sources, key = { it.id }) { source ->
                ContentSourceCard(
                    source = source,
                    validationResult = validationFeedback[source.id],
                    strings = strings,
                    onToggleEnabled = { enabled -> onToggleSourceEnabled(source, enabled) },
                    onTestConnection = { onTestConnection(source) },
                    onSyncNow = { onSyncSource(source.id) },
                    onEdit = {
                        editingSource = source
                        showAddOrEditDialog = true
                    },
                    onRemove = { sourcePendingDeletion = source }
                )
            }

            // Sync History Section
            if (syncLogs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = KrasSilver)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strings.isArabic) "سجل المزامنة التلقائية واليدوية" else "Synchronization History",
                            style = MaterialTheme.typography.titleLarge,
                            color = KrasWhite
                        )
                    }
                }

                items(syncLogs.take(10), key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KrasObsidian),
                        border = BorderStroke(1.dp, KrasCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (log.isSuccess) KrasEmerald else KrasError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.sourceName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = KrasWhite
                                    )
                                    Text(
                                        text = formatTimestamp(log.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KrasMuted
                                    )
                                }
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrasSilver
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Source Modal
    if (showAddOrEditDialog) {
        AddOrEditSourceDialog(
            initialSource = editingSource,
            strings = strings,
            onDismiss = { showAddOrEditDialog = false },
            onSave = { updated ->
                showAddOrEditDialog = false
                if (editingSource == null) {
                    onAddSource(updated)
                } else {
                    onUpdateSource(updated)
                }
            }
        )
    }

    // Remove Source Confirmation Dialog
    if (sourcePendingDeletion != null) {
        val target = sourcePendingDeletion!!
        AlertDialog(
            onDismissRequest = { sourcePendingDeletion = null },
            containerColor = KrasObsidian,
            titleContentColor = KrasWhite,
            textContentColor = KrasSilver,
            title = {
                Text(
                    text = if (strings.isArabic) "تأكيد حذف المصدر" else "Remove Content Source?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = if (strings.isArabic) {
                        "سيؤدي حذف '${target.name}' إلى إزالة ${target.importedMovieCount} فيلم و ${target.importedSeriesCount} مسلسل مرتبط بهذا المصدر من الكتالوج الموحد. هل تريد المتابعة؟"
                    } else {
                        "Removing '${target.name}' will permanently remove its ${target.importedMovieCount} imported movies and ${target.importedSeriesCount} series (including episodes) from your unified catalog. Continue?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSource(target)
                        sourcePendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KrasError)
                ) {
                    Text(strings.removeSource, color = KrasWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { sourcePendingDeletion = null }) {
                    Text(if (strings.isArabic) "إلغاء" else "Cancel", color = KrasSilver)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentSourceCard(
    source: ContentSourceEntity,
    validationResult: ConnectionValidationResult?,
    strings: AppStrings,
    onToggleEnabled: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val typeEnum = runCatching { SourceType.valueOf(source.sourceType) }.getOrDefault(SourceType.JSON_FEED)
    val statusColor = when (source.connectionStatus) {
        "CONNECTED" -> KrasEmerald
        "SYNCING" -> KrasTeal
        "ERROR" -> KrasError
        "DISABLED" -> KrasMuted
        else -> KrasAmber
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
        border = BorderStroke(1.dp, if (source.isSampleSource) KrasGold.copy(alpha = 0.5f) else KrasCardBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = KrasWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (strings.isArabic) typeEnum.displayNameAr else typeEnum.displayName} • ${source.url}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrasMuted
                    )
                }

                Switch(
                    checked = source.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KrasWhite,
                        checkedTrackColor = KrasRed,
                        uncheckedThumbColor = KrasSilver,
                        uncheckedTrackColor = KrasObsidian
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row: Movies Count | Series Count | Sync Interval | Last Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill(
                    label = if (strings.isArabic) "أفلام" else "Movies",
                    value = "${source.importedMovieCount}",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = if (strings.isArabic) "مسلسلات" else "Series",
                    value = "${source.importedSeriesCount}",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = if (strings.isArabic) "المزامنة" else "Interval",
                    value = "Every ${source.syncIntervalHours}h",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (source.lastSyncTimestamp > 0) {
                    "Last Successful Sync: ${formatTimestamp(source.lastSyncTimestamp)}"
                } else {
                    "Not yet synchronized"
                },
                style = MaterialTheme.typography.labelSmall,
                color = KrasSilver
            )

            // Sync Progress Indicator
            if (source.connectionStatus == "SYNCING" || (source.syncProgress > 0f && source.syncProgress < 1f)) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { source.syncProgress.coerceIn(0.1f, 1f) },
                    color = KrasTeal,
                    trackColor = KrasObsidian,
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                )
            }

            // Detected Capabilities
            val caps = source.detectedCapabilities.split(",").filter { it.isNotBlank() }
            if (caps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    caps.forEach { cap ->
                        Surface(
                            color = KrasObsidian,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, KrasCardBorder)
                        ) {
                            Text(
                                text = cap.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasSilver,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Error Diagnostics
            if (!source.lastSyncError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = KrasError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, KrasError.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = KrasError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = source.lastSyncError,
                            style = MaterialTheme.typography.bodySmall,
                            color = KrasWhite
                        )
                    }
                }
            }

            // Live Test Connection Result
            if (validationResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val vColor = if (validationResult.isSuccess) KrasEmerald else KrasError
                Surface(
                    color = vColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, vColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = validationResult.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = KrasWhite,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Test Connection | Sync Now | Edit | Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTestConnection,
                        border = BorderStroke(1.dp, KrasCardBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = KrasTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.testConnection, style = MaterialTheme.typography.labelMedium, color = KrasWhite)
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = source.isEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = KrasRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.syncNow, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editSource, tint = KrasSilver)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.removeSource, tint = KrasError)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = KrasObsidian,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, KrasCardBorder),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = KrasWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = KrasMuted
            )
        }
    }
}

@Composable
fun AddOrEditSourceDialog(
    initialSource: ContentSourceEntity?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (ContentSourceEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialSource?.name ?: "") }
    var url by remember { mutableStateOf(initialSource?.url ?: "") }
    var selectedType by remember {
        mutableStateOf(
            initialSource?.sourceType?.let { runCatching { SourceType.valueOf(it) }.getOrNull() }
                ?: SourceType.JSON_FEED
        )
    }
    var apiKey by remember { mutableStateOf(initialSource?.apiKey ?: "") }
    var username by remember { mutableStateOf(initialSource?.username ?: "") }
    var password by remember { mutableStateOf(initialSource?.password ?: "") }
    var syncIntervalHours by remember { mutableIntStateOf(initialSource?.syncIntervalHours ?: 6) }
    var preferredLanguage by remember { mutableStateOf(initialSource?.preferredLanguage ?: "en") }
    var categories by remember {
        mutableStateOf(initialSource?.allowedCategories ?: "Movies,Series,Action,Sci-Fi,Animation")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = KrasObsidian),
            border = BorderStroke(1.dp, KrasCardBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialSource == null) strings.addSource else strings.editSource,
                    style = MaterialTheme.typography.headlineSmall,
                    color = KrasWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quick Presets (One-Tap Verified Authorized Feeds):",
                    style = MaterialTheme.typography.labelMedium,
                    color = KrasGold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick-fill verified feed presets so users can test JSON, M3U, and RSS connectors immediately!
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        Triple("Open Broadcast JSON", "kras://open-json-feed", SourceType.JSON_FEED),
                        Triple("Open M3U8 VOD Playlist", "kras://open-m3u-playlist", SourceType.M3U_PLAYLIST),
                        Triple("Open Cinema RSS 2.0", "kras://open-rss-feed", SourceType.XML_RSS_FEED)
                    )
                    items(presets) { (presetName, presetUrl, presetType) ->
                        Surface(
                            color = KrasCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, KrasGold.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                name = presetName
                                url = presetUrl
                                selectedType = presetType
                                validationError = null
                            }
                        ) {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KrasRed,
                        unfocusedBorderColor = KrasCardBorder,
                        focusedTextColor = KrasWhite,
                        unfocusedTextColor = KrasWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("source_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        selectedType = SourceConnectorRegistry.detectSourceTypeFromUrl(it)
                    },
                    label = { Text("HTTPS API / Feed / Playlist URL") },
                    placeholder = { Text("https://api.example.com/catalog.json") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KrasRed,
                        unfocusedBorderColor = KrasCardBorder,
                        focusedTextColor = KrasWhite,
                        unfocusedTextColor = KrasWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("source_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Source Integration Type (Auto-Detected):",
                    style = MaterialTheme.typography.labelMedium,
                    color = KrasSilver
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SourceType.entries) { type ->
                        val selected = type == selectedType
                        Surface(
                            color = if (selected) KrasRed else KrasCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) KrasRed else KrasCardBorder),
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Optional API Key / Bearer Token") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KrasRed,
                        unfocusedBorderColor = KrasCardBorder,
                        focusedTextColor = KrasWhite,
                        unfocusedTextColor = KrasWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == SourceType.XTREAM_API || selectedType == SourceType.REST_API) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KrasRed,
                                unfocusedBorderColor = KrasCardBorder,
                                focusedTextColor = KrasWhite,
                                unfocusedTextColor = KrasWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KrasRed,
                                unfocusedBorderColor = KrasCardBorder,
                                focusedTextColor = KrasWhite,
                                unfocusedTextColor = KrasWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sync Interval & Language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Sync Interval:", style = MaterialTheme.typography.labelMedium, color = KrasSilver)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 6, 12, 24).forEach { hrs ->
                            Surface(
                                color = if (syncIntervalHours == hrs) KrasRed else KrasCharcoal,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { syncIntervalHours = hrs }
                            ) {
                                Text(
                                    text = "${hrs}h",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KrasWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = { Text("Content Categories (comma-separated)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KrasRed,
                        unfocusedBorderColor = KrasCardBorder,
                        focusedTextColor = KrasWhite,
                        unfocusedTextColor = KrasWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = KrasError
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = KrasSilver)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || url.isBlank()) {
                                validationError = "Please enter both a Source Name and a valid HTTPS or kras:// URL."
                                return@Button
                            }
                            if (!url.trim().startsWith("https://", ignoreCase = true) &&
                                !url.trim().startsWith("kras://", ignoreCase = true)
                            ) {
                                validationError = "Insecure HTTP URLs are rejected. Please use an HTTPS URL."
                                return@Button
                            }
                            onSave(
                                (initialSource ?: ContentSourceEntity(
                                    name = name.trim(),
                                    url = url.trim(),
                                    sourceType = selectedType.name
                                )).copy(
                                    name = name.trim(),
                                    url = url.trim(),
                                    sourceType = selectedType.name,
                                    apiKey = apiKey.trim(),
                                    username = username.trim(),
                                    password = password.trim(),
                                    syncIntervalHours = syncIntervalHours,
                                    preferredLanguage = preferredLanguage,
                                    allowedCategories = categories.trim(),
                                    isSampleSource = selectedType == SourceType.LEGAL_DEMO_CATALOG
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KrasRed),
                        modifier = Modifier.testTag("save_source_button")
                    ) {
                        Text("Save & Synchronize")
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return "Never"
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
