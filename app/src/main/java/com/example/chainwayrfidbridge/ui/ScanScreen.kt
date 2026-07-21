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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import com.example.chainwayrfidbridge.ScanUiState
import com.example.chainwayrfidbridge.ScanViewModel
import com.example.chainwayrfidbridge.SendStatus
import com.example.chainwayrfidbridge.SortOption
import com.example.chainwayrfidbridge.data.TagRecord
import com.example.chainwayrfidbridge.ui.theme.ErrorRed
import com.example.chainwayrfidbridge.ui.theme.NewTagHighlight
import com.example.chainwayrfidbridge.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stechoq RFID Suite", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
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
                        "Belum ada tag terbaca",
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
                            Toast.makeText(context, "EPC disalin", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
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
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                StatItem("Total Terdeteksi", state.totalReads.toString(), Modifier.weight(1f))
                StatItem("Total Unik", state.tags.size.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan terakhir: ${formatTime(state.lastScanTime)}",
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
                    Text("Stop Scan")
                }

                state.tags.isEmpty() -> Button(
                    onClick = onToggleScan,
                    enabled = state.readerConnected,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mulai Scan")
                }

                else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartNew,
                        enabled = state.readerConnected,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Baru")
                    }
                    Button(
                        onClick = onToggleScan,
                        enabled = state.readerConnected,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lanjutkan")
                    }
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
private fun SendStatusBanner(status: SendStatus, onRetry: () -> Unit) {
    when (status) {
        is SendStatus.Sending -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text("Mengirim...", style = MaterialTheme.typography.bodySmall)
        }
        is SendStatus.Success -> Text(
            "Terkirim (${status.count} tag)",
            color = SuccessGreen,
            style = MaterialTheme.typography.bodySmall
        )
        is SendStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Gagal kirim: ${status.message}",
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("Retry", style = MaterialTheme.typography.labelMedium)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompactSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(4.dp))
        Box {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.Sort, contentDescription = "Urutkan")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { onSortChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
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
                        "Cari EPC...",
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
                        maxLines = 1
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tag.isNew) "BARU" else "LAMA",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tag.isNew) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "R:${tag.readCount}  RSSI:${tag.rssi}  Ant:${tag.antenna}  ${formatTime(tag.lastSeen)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy EPC",
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
