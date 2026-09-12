package com.example.chainwayrfidbridge.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chainwayrfidbridge.ScanUiState
import com.example.chainwayrfidbridge.ScanViewModel
import com.example.chainwayrfidbridge.SendStatus
import com.example.chainwayrfidbridge.SortOption
import com.example.chainwayrfidbridge.UpdateStatus
import com.example.chainwayrfidbridge.data.BarcodeScanRecord
import com.example.chainwayrfidbridge.data.BarcodeSendStatus
import com.example.chainwayrfidbridge.data.InputMode
import com.example.chainwayrfidbridge.data.TagQuality
import com.example.chainwayrfidbridge.data.TagRecord
import com.example.chainwayrfidbridge.network.formatSendError
import com.example.chainwayrfidbridge.ui.theme.BarcodeAccent
import com.example.chainwayrfidbridge.ui.theme.BluePrimary
import com.example.chainwayrfidbridge.ui.theme.ErrorRed
import com.example.chainwayrfidbridge.ui.theme.NewTagHighlight
import com.example.chainwayrfidbridge.ui.theme.SuccessGreen
import com.example.chainwayrfidbridge.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val strings = LocalStrings.current
    val isBarcodeMode = config.inputMode == InputMode.BARCODE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appTitle, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = strings.settingsDescription)
                    }
                },
                // Distinct accent color is the main visual cue that Barcode mode is active,
                // since the two modes otherwise share the same screen chrome.
                colors = if (isBarcodeMode) {
                    TopAppBarDefaults.topAppBarColors(containerColor = BarcodeAccent.copy(alpha = 0.12f))
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (state.updateStatus !is UpdateStatus.Idle) {
                UpdateBanner(
                    status = state.updateStatus,
                    onDownload = { viewModel.downloadUpdate() },
                    onInstall = { viewModel.installUpdate() }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (isBarcodeMode) {
                BarcodeScanBody(state = state, onCopy = { code ->
                    clipboard.setText(AnnotatedString(code))
                    Toast.makeText(context, strings.epcCopiedToast, Toast.LENGTH_SHORT).show()
                })
            } else {
                SummaryCard(
                    state = state,
                    onToggleScan = { viewModel.toggleScan() },
                    onStartNew = { viewModel.startNewScan() },
                    onRetrySend = { viewModel.retrySend() }
                )

                Spacer(Modifier.height(12.dp))

                SearchSortRow(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    sortOption = state.sortOption,
                    onSortChange = viewModel::setSortOption
                )

                Spacer(Modifier.height(10.dp))

                if (state.tags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            strings.emptyTagList,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.tags.firstOrNull()?.epc) {
                        if (state.tags.isNotEmpty()) listState.animateScrollToItem(0)
                    }
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.tags, key = { it.epc }) { tag ->
                            TagRow(tag) { epc ->
                                clipboard.setText(AnnotatedString(epc))
                                Toast.makeText(context, strings.epcCopiedToast, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeScanBody(state: ScanUiState, onCopy: (String) -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BarcodeAccent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = BarcodeAccent)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(strings.totalScanned, style = MaterialTheme.typography.bodySmall)
                    Text(
                        state.barcodeScans.size.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                strings.barcodeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    if (state.barcodeScans.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                strings.barcodeEmptyList,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.barcodeScans, key = { it.timestamp }) { scan ->
                BarcodeRow(scan, onCopy)
            }
        }
    }
}

@Composable
private fun BarcodeRow(scan: BarcodeScanRecord, onCopy: (String) -> Unit) {
    val strings = LocalStrings.current
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    scan.code,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatTime(scan.timestamp)}  ${
                        when (scan.status) {
                            BarcodeSendStatus.SENDING -> strings.barcodeSendingLabel
                            BarcodeSendStatus.SENT -> strings.barcodeSentLabel
                            BarcodeSendStatus.FAILED -> strings.barcodeFailedLabel
                        }
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (scan.status) {
                        BarcodeSendStatus.SENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        BarcodeSendStatus.SENT -> SuccessGreen
                        BarcodeSendStatus.FAILED -> ErrorRed
                    }
                )
            }
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = strings.copyEpcDescription,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onCopy(scan.code) }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    state: ScanUiState,
    onToggleScan: () -> Unit,
    onStartNew: () -> Unit,
    onRetrySend: () -> Unit
) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                StatItem(strings.totalDetected, state.totalReads.toString(), Modifier.weight(1f))
                StatItem(strings.totalUnique, state.tags.size.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${strings.lastScanPrefix}${formatTime(state.lastScanTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            when {
                state.scanning -> Button(
                    onClick = onToggleScan,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.stopScan)
                }

                state.tags.isEmpty() -> Button(
                    onClick = onToggleScan,
                    enabled = state.readerConnected,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.startScan)
                }

                // No icons and tight horizontal padding here on purpose: the default
                // ButtonDefaults.ContentPadding (24dp each side) plus an icon leaves too little
                // room for the label on a half-width button on narrower screens (e.g. Zebra
                // handhelds), which wrapped "New Scan"/"Continue" onto a clipped second line.
                else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartNew,
                        enabled = state.readerConnected,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(strings.scanNew, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onToggleScan,
                        enabled = state.readerConnected,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(strings.continueScan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            AnimatedVisibility(visible = state.scanStartError != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${strings.scanStartErrorPrefix}${state.scanStartError.orEmpty()}",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            AnimatedVisibility(visible = state.sendStatus !is SendStatus.Idle) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SendStatusBanner(state.sendStatus, onRetrySend)
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(status: UpdateStatus, onDownload: () -> Unit, onInstall: () -> Unit) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                is UpdateStatus.Available -> {
                    Text(
                        "${strings.updateAvailablePrefix}${status.info.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDownload, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(strings.updateNowButton, style = MaterialTheme.typography.labelMedium)
                    }
                }
                UpdateStatus.Downloading -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.downloadingUpdate, style = MaterialTheme.typography.bodyMedium)
                }
                is UpdateStatus.ReadyToInstall -> {
                    Text(
                        strings.updateReadyToInstall,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onInstall, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(strings.installUpdateButton, style = MaterialTheme.typography.labelMedium)
                    }
                }
                is UpdateStatus.Error -> {
                    Text(
                        "${strings.updateErrorPrefix}${status.message}",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onDownload, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(strings.retry, style = MaterialTheme.typography.labelMedium)
                    }
                }
                UpdateStatus.Idle -> {}
            }
        }
    }
}

@Composable
private fun SendStatusBanner(status: SendStatus, onRetry: () -> Unit) {
    val strings = LocalStrings.current
    when (status) {
        is SendStatus.Sending -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text(strings.sending, style = MaterialTheme.typography.bodySmall)
        }
        is SendStatus.Success -> Text(
            strings.sentSuccess(status.count),
            color = SuccessGreen,
            style = MaterialTheme.typography.bodySmall
        )
        is SendStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
            val display = formatSendError(status.message, strings.systemErrorGeneric)
            Text(
                display.message,
                color = if (display.isSystemError) ErrorRed else BluePrimary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text(strings.retry, style = MaterialTheme.typography.labelMedium)
            }
        }
        SendStatus.Idle -> {}
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(4.dp))
        Box {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.Sort, contentDescription = strings.sortDescription)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(strings.sortLabel(option)) },
                        onClick = { onSortChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        strings.searchPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

@Composable
private fun TagRow(tag: TagRecord, onCopy: (String) -> Unit) {
    val strings = LocalStrings.current
    var highlight by remember(tag.epc) { mutableStateOf(tag.isNew) }
    LaunchedEffect(tag.epc) {
        if (tag.isNew) {
            delay(1200)
            highlight = false
        }
    }
    val bgColor by animateColorAsState(
        if (highlight) NewTagHighlight else MaterialTheme.colorScheme.surface,
        label = "tagHighlight"
    )

    Card(colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tag.epc,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Weighted so the badge on the right always keeps its natural width and
                        // never wraps — without this the EPC (long) claims full row width first,
                        // squeezing "EXISTING" into too little room and forcing it onto two lines.
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tag.isNew) strings.tagNew else strings.tagExisting,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tag.isNew) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                val quality = remember(tag.rssi, tag.readCount) { TagQuality.from(tag.rssi, tag.readCount) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "R:${tag.readCount}  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        "${strings.qualityLabel}${strings.qualityText(quality)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (quality) {
                            TagQuality.STRONG -> SuccessGreen
                            TagQuality.MEDIUM -> WarningAmber
                            TagQuality.WEAK -> ErrorRed
                        },
                        maxLines = 1
                    )
                }
            }
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = strings.copyEpcDescription,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onCopy(tag.epc) }
            )
        }
    }
}

private fun formatTime(millis: Long?): String {
    if (millis == null) return "-"
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))
}
