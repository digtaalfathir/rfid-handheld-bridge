package com.stechoq.wms.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stechoq.wms.BuildConfig
import com.stechoq.wms.DropdownFetchState
import com.stechoq.wms.ScanViewModel
import com.stechoq.wms.UpdateStatus
import com.stechoq.wms.data.AppLanguage
import com.stechoq.wms.data.FactoryCodeOption
import com.stechoq.wms.data.InputMode
import com.stechoq.wms.data.ScanConfig
import com.stechoq.wms.data.ScanMode
import com.stechoq.wms.data.SendContext
import com.stechoq.wms.data.SendLogEntry
import com.stechoq.wms.data.SendType
import com.stechoq.wms.data.ValidationErrorType
import com.stechoq.wms.ui.theme.BarcodeAccent
import com.stechoq.wms.ui.theme.ErrorRed
import com.stechoq.wms.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// How long the operator has to stop typing in Base URL before RR Type / Factory Code are fetched —
// the field fires per keystroke, and every partial URL along the way isn't worth a request.
private const val BASE_URL_DEBOUNCE_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScanViewModel,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var draft by remember { mutableStateOf(viewModel.config.value) }
    var errors by remember { mutableStateOf(emptyMap<String, ValidationErrorType>()) }
    var showLogHistory by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val powerRange = remember { viewModel.powerRange }
    val antennaOptions = remember { viewModel.antennaOptions() }
    var rrTypeOptions by remember { mutableStateOf(viewModel.rrTypeOptions()) }
    val baseUrlOptions = remember { viewModel.baseUrlOptions() }
    val endpointOptions = remember { viewModel.endpointOptions() }
    val initialYearOptions = remember { viewModel.initialYearOptions() }
    var factoryCodeOptions by remember { mutableStateOf(viewModel.factoryCodeOptions()) }
    val fetchState by viewModel.dropdownFetch.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    // Set by a pick from the Base URL menu, so the effect below fetches right away instead of
    // waiting out the typing debounce.
    var skipDebounce by remember { mutableStateOf(false) }
    var isFirstBaseUrlEffect by remember { mutableStateOf(true) }

    LaunchedEffect(draft.baseUrl) {
        // Any change cancels the fetch in flight, and any status left from an earlier visit.
        viewModel.cancelDropdownFetch()
        if (isFirstBaseUrlEffect) {
            isFirstBaseUrlEffect = false
            // Opening Settings isn't a Base URL change — only fetch when there's nothing cached
            // to show (fresh install, or after Reset).
            val cacheEmpty = rrTypeOptions.isEmpty() || factoryCodeOptions.isEmpty()
            if (cacheEmpty && ScanConfig.isFetchableBaseUrl(draft.baseUrl)) {
                viewModel.fetchDropdownData(draft.baseUrl)
            }
            return@LaunchedEffect
        }
        // Typing again restarts this effect, which cancels the delay — so only the last
        // keystroke's URL ever gets fetched.
        if (!skipDebounce) delay(BASE_URL_DEBOUNCE_MS)
        skipDebounce = false
        if (ScanConfig.isFetchableBaseUrl(draft.baseUrl)) {
            viewModel.fetchDropdownData(draft.baseUrl)
        }
    }

    LaunchedEffect(fetchState) {
        val state = fetchState
        if (state is DropdownFetchState.Success && state.baseUrl == draft.baseUrl) {
            rrTypeOptions = state.rrTypes
            factoryCodeOptions = state.factoryCodes
            draft = draft.keepingOnlyAvailable(state.rrTypes, state.factoryCodes)
        }
    }

    // Unsaved = the draft differs from what's actually saved. This includes edits Settings made
    // on its own (e.g. a Base URL change clearing an RR Type the new server doesn't have) — those
    // are just as lost on leaving without Save.
    val savedConfig by viewModel.config.collectAsState()
    val hasUnsavedChanges = draft != savedConfig
    var showUnsavedConfirm by remember { mutableStateOf(false) }

    // Validation still applies however Save is reached; on failure stay here with errors shown.
    fun saveAndExit() {
        val result = viewModel.saveConfig(draft)
        errors = result
        if (result.isEmpty()) {
            Toast.makeText(context, strings.configSavedToast, Toast.LENGTH_SHORT).show()
            onDone()
        } else {
            Toast.makeText(context, strings.configInvalidToast, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestExit() {
        if (hasUnsavedChanges) showUnsavedConfirm = true else onDone()
    }

    // The Android back button/gesture, same as the top bar's arrow.
    BackHandler { requestExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { requestExit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = strings.backDescription)
                    }
                }
            )
        },
        // Pinned so Save/Reset never need a scroll to the bottom of this long page.
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedButton(
                        // Wipes every setting and the dropdown cache in one tap — always confirmed first.
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.reset)
                    }
                    Button(
                        onClick = { saveAndExit() },
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Where the Scan screen's red dot on the Settings icon points: the update banner the
            // operator put off with "Later" stays reachable here, at the top, without a restart.
            if (uiState.updateStatus !is UpdateStatus.Idle) {
                UpdateBanner(
                    status = uiState.updateStatus,
                    onDownload = { viewModel.downloadUpdate() },
                    onInstall = { viewModel.installUpdate() }
                )
                Spacer(Modifier.height(12.dp))
            }

            SectionCard(
                title = strings.scanModeTitle,
                icon = Icons.Filled.QrCodeScanner,
                accent = if (draft.inputMode == InputMode.BARCODE) BarcodeAccent else null
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputMode.entries.forEach { inputMode ->
                        val selected = draft.inputMode == inputMode
                        if (selected) {
                            Button(
                                onClick = { draft = draft.copy(inputMode = inputMode) },
                                modifier = Modifier.weight(1f),
                                colors = if (inputMode == InputMode.BARCODE) {
                                    ButtonDefaults.buttonColors(containerColor = BarcodeAccent)
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text(strings.scanModeLabel(inputMode))
                            }
                        } else {
                            OutlinedButton(onClick = { draft = draft.copy(inputMode = inputMode) }, modifier = Modifier.weight(1f)) {
                                Text(strings.scanModeLabel(inputMode))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.modeTitle, icon = Icons.Filled.SwapHoriz) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanMode.entries.forEach { mode ->
                        val selected = draft.mode == mode
                        if (selected) {
                            Button(onClick = { draft = draft.copy(mode = mode) }, modifier = Modifier.weight(1f)) {
                                Text(mode.label)
                            }
                        } else {
                            OutlinedButton(onClick = { draft = draft.copy(mode = mode) }, modifier = Modifier.weight(1f)) {
                                Text(mode.label)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = draft.opname,
                        // Replacement always posts as an opname-style update — forced on and
                        // locked while Replacement is active so the two can't silently disagree;
                        // unlocked again the moment Replacement is turned back off.
                        enabled = !draft.replacementModeEnabled,
                        onCheckedChange = { draft = draft.copy(opname = it) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.opnameLabel, style = MaterialTheme.typography.bodyMedium)
                }
                // Replacement is a Register-specific workflow (swapping a damaged tag's server
                // record for a new physical tag) — hidden in WO the same way opname used to be.
                if (draft.mode == ScanMode.REGISTER) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = draft.replacementModeEnabled,
                            onCheckedChange = { checked ->
                                draft = draft.copy(
                                    replacementModeEnabled = checked,
                                    opname = if (checked) true else draft.opname
                                )
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.replacementModeLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        strings.replacementModeDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp, end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.apiConfigTitle, icon = Icons.Filled.Cloud) {
                DropdownField(
                    strings.baseUrlLabel,
                    draft.baseUrl,
                    baseUrlOptions,
                    errors["baseUrl"]?.let(strings::validationMessage),
                    onOptionSelected = { picked ->
                        if (picked == draft.baseUrl) {
                            // Unchanged, so the effect won't re-run — treat the pick as a refresh.
                            viewModel.fetchDropdownData(picked)
                        } else {
                            skipDebounce = true
                            draft = draft.copy(baseUrl = picked)
                        }
                    }
                ) {
                    draft = draft.copy(baseUrl = it)
                }
                DropdownFetchStatus(fetchState, onRetry = { viewModel.fetchDropdownData(draft.baseUrl) })
                Spacer(Modifier.height(8.dp))
                DropdownField(strings.endpointLabel, draft.endpoint, endpointOptions, errors["endpoint"]?.let(strings::validationMessage)) {
                    draft = draft.copy(endpoint = it)
                }
                Spacer(Modifier.height(8.dp))
                ReadOnlyField(strings.readerIdLabel, draft.readerId)
                Spacer(Modifier.height(8.dp))
                DropdownField(strings.antennaLabel, draft.antenna, antennaOptions, errors["antenna"]?.let(strings::validationMessage)) {
                    draft = draft.copy(antenna = it)
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.registerConfigTitle, icon = Icons.Filled.Description) {
                SelectOnlyDropdownField(strings.rrTypeLabel, draft.rrType, rrTypeOptions.map { it to it }, errors["rrType"]?.let(strings::validationMessage)) {
                    draft = draft.copy(rrType = it)
                }
                Spacer(Modifier.height(8.dp))
                LabeledField(strings.makerNameLabel, draft.makerName, errors["makerName"]?.let(strings::validationMessage)) {
                    draft = draft.copy(makerName = it)
                }
                Spacer(Modifier.height(8.dp))
                DropdownField(strings.initialYearLabel, draft.initialYear, initialYearOptions, errors["initialYear"]?.let(strings::validationMessage)) {
                    draft = draft.copy(initialYear = it)
                }
                Spacer(Modifier.height(8.dp))
                SelectOnlyDropdownField(
                    strings.factoryCodeLabel,
                    draft.factoryCode,
                    factoryCodeOptions.map { it.code to "${it.code} - ${it.name}" },
                    errors["factoryCode"]?.let(strings::validationMessage)
                ) { selectedCode ->
                    // factoryId rides along with factoryCode — the RFID-replacement endpoint
                    // wants the numeric id, the regular scan payload wants the code string, both
                    // from this same fetched entry.
                    val matched = factoryCodeOptions.find { it.code == selectedCode }
                    draft = draft.copy(factoryCode = selectedCode, factoryId = matched?.id ?: draft.factoryId)
                }
            }

            Spacer(Modifier.height(12.dp))

            // RF transmit power has no meaning for barcode scanning — hidden rather than shown
            // disabled, since it'd otherwise be dead weight on every Barcode-mode settings visit.
            if (draft.inputMode == InputMode.RFID) {
                SectionCard(title = strings.powerTitle, icon = Icons.Filled.Bolt) {
                    Text("${strings.powerLevelPrefix}${draft.power}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = draft.power.toFloat(),
                        onValueChange = { draft = draft.copy(power = it.roundToInt()) },
                        valueRange = powerRange.first.toFloat()..powerRange.last.toFloat(),
                        steps = powerRange.last - powerRange.first - 1
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            SectionCard(title = strings.soundTitle, icon = Icons.Filled.VolumeUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.soundToggleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = draft.soundEnabled, onCheckedChange = { draft = draft.copy(soundEnabled = it) })
                }
                if (draft.soundEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(strings.soundVolumeLabel, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = draft.soundVolume.toFloat(),
                        onValueChange = { draft = draft.copy(soundVolume = it.roundToInt()) },
                        valueRange = ScanConfig.MIN_VOLUME.toFloat()..ScanConfig.MAX_VOLUME.toFloat()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.localCsvTitle, icon = Icons.Filled.Backup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.localCsvToggleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = draft.localCsvEnabled, onCheckedChange = { draft = draft.copy(localCsvEnabled = it) })
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.backgroundScanTitle, icon = Icons.Filled.Layers) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.backgroundScanToggleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = draft.backgroundScanEnabled,
                        onCheckedChange = { enabled ->
                            draft = draft.copy(backgroundScanEnabled = enabled)
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, strings.overlayPermissionToast, Toast.LENGTH_LONG).show()
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.languageTitle, icon = Icons.Filled.Language) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val label = if (lang == AppLanguage.EN) strings.languageEnglish else strings.languageIndonesian
                        if (lang == language) {
                            Button(onClick = { onLanguageChange(lang) }, modifier = Modifier.weight(1f)) {
                                Text(label)
                            }
                        } else {
                            OutlinedButton(onClick = { onLanguageChange(lang) }, modifier = Modifier.weight(1f)) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.logHistoryTitle, icon = Icons.Filled.History) {
                OutlinedButton(onClick = { showLogHistory = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.logHistoryTitle)
                }
            }

            if (showLogHistory) {
                LogHistoryDialog(viewModel = viewModel, onDismiss = { showLogHistory = false })
            }

            if (showResetConfirm) {
                ResetConfirmDialog(
                    onConfirm = {
                        showResetConfirm = false
                        draft = viewModel.resetConfig()
                        errors = emptyMap()
                        // Reset wipes the cached lists too — without this the dropdowns would
                        // keep showing them until Settings is reopened.
                        rrTypeOptions = viewModel.rrTypeOptions()
                        factoryCodeOptions = viewModel.factoryCodeOptions()
                        viewModel.cancelDropdownFetch()
                        Toast.makeText(context, strings.configResetToast, Toast.LENGTH_SHORT).show()
                    },
                    onCancel = { showResetConfirm = false }
                )
            }

            if (showUnsavedConfirm) {
                UnsavedChangesDialog(
                    onSave = {
                        showUnsavedConfirm = false
                        saveAndExit()
                    },
                    onDiscard = {
                        showUnsavedConfirm = false
                        onDone()
                    },
                    onCancel = { showUnsavedConfirm = false }
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                // e.g. "Versi 1.1 · Update tersedia: v1.5.15" — the banner at the top is where
                // to start it; this just says so where the version is naturally looked up.
                listOfNotNull(
                    "${strings.versionPrefix}${BuildConfig.VERSION_NAME}",
                    uiState.newerVersion?.let { "${strings.updateAvailablePrefix}$it" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector? = null, accent: Color? = null, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, error: String?, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * A plain, always-editable text field paired with a suggestions menu — deliberately not
 * ExposedDropdownMenuBox, which intercepts taps on the field to reopen the menu instead of
 * letting the user place a cursor, so a picked value could never be cleared to type something
 * outside the list. The icon button is the only thing that opens/closes the menu; the field
 * itself behaves like any other text field at all times.
 *
 * [onOptionSelected] lets a caller tell a menu pick apart from typing (Base URL fetches on a pick
 * immediately, but debounces typing); left null, a pick goes through [onValueChange] like typing.
 */
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    error: String?,
    onOptionSelected: ((String) -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        (onOptionSelected ?: onValueChange)(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** The RR Type / Factory Code fetch status shown under Base URL — replaces the old Toast, which
 * vanished before an operator could read it and left no way to retry. */
@Composable
private fun DropdownFetchStatus(state: DropdownFetchState, onRetry: () -> Unit) {
    val strings = LocalStrings.current
    when (state) {
        DropdownFetchState.Idle -> Unit
        is DropdownFetchState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                strings.dropdownFetchLoading,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is DropdownFetchState.Success -> Text(
            strings.dropdownFetchConnected(state.rrTypes.size, state.factoryCodes.size),
            style = MaterialTheme.typography.bodySmall,
            color = SuccessGreen
        )
        is DropdownFetchState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.dropdownFetchFailed, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                if (state.hasStaleCache) {
                    Text(
                        strings.dropdownFetchStaleHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onRetry) { Text(strings.tryAgain) }
        }
    }
}

/** Same shape as [DropdownField] but selection-only — no typing, since these options come from
 * the server (RR Type, Factory Code) rather than free text an operator could reasonably type by
 * hand. [options] is value-to-display-label so Factory Code can show "code - name" while [value]
 * (and what [onSelect] receives) stays just the code. */
@Composable
private fun SelectOnlyDropdownField(label: String, value: String, options: List<Pair<String, String>>, error: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optValue, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel) },
                    onClick = {
                        onSelect(optValue)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Leaving Settings (arrow or Android back) with edits not yet saved. */
@Composable
private fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onCancel: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(strings.unsavedChangesTitle) },
        text = { Text(strings.unsavedChangesMessage) },
        confirmButton = {
            Button(onClick = onSave) { Text(strings.save) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) { Text(strings.cancel) }
                TextButton(onClick = onDiscard) { Text(strings.discard, color = ErrorRed) }
            }
        }
    )
}

/** Reset clears every setting (Base URL, Factory Code, …) plus the dropdown cache, so a stray
 * tap mustn't do it outright. Cancel leaves everything — including unsaved edits — untouched. */
@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(strings.resetConfirmTitle) },
        text = { Text(strings.resetConfirmMessage) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                Text(strings.reset)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(strings.cancel) }
        }
    )
}

// Long batch payloads are cut off on screen — the stored body (and what a resend posts) is whole.
private const val LOG_PAYLOAD_DISPLAY_LIMIT = 2000

/** The full/raw detail behind whatever generalized message the Scan screen showed at the time —
 * so a system-error banner ("contact WMS Team") doesn't leave the actual cause unrecoverable —
 * plus what each send was (type, config, exact payload) and a way to resend it as it went out. */
@Composable
private fun LogHistoryDialog(viewModel: ScanViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val timeFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.US) }
    var entries by remember { mutableStateOf(viewModel.sendLog()) }
    var expanded by remember { mutableStateOf(emptySet<SendLogEntry>()) }
    var confirming by remember { mutableStateOf<SendLogEntry?>(null) }
    var resending by remember { mutableStateOf<SendLogEntry?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.logHistoryTitle) },
        text = {
            if (entries.isEmpty()) {
                Text(strings.logHistoryEmpty, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    entries.forEachIndexed { index, entry ->
                        LogEntryItem(
                            entry = entry,
                            timeFormat = timeFormat,
                            expanded = entry in expanded,
                            payload = { viewModel.sendLogPayload(entry) },
                            resending = resending == entry,
                            resendEnabled = resending == null,
                            onToggle = { expanded = if (entry in expanded) expanded - entry else expanded + entry },
                            onResend = { confirming = entry }
                        )
                        if (index != entries.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )

    confirming?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(strings.logResendConfirmTitle) },
            text = {
                Column {
                    Text(strings.logResendConfirmMessage, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.context?.url.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    if (entry.success) {
                        Spacer(Modifier.height(8.dp))
                        Text(strings.logResendDuplicateWarning, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirming = null
                    resending = entry
                    viewModel.resendLogEntry(entry) { success ->
                        resending = null
                        entries = viewModel.sendLog()
                        val message = if (success) strings.logResendSuccessToast else strings.logResendFailedToast
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(strings.logResend) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun LogEntryItem(
    entry: SendLogEntry,
    timeFormat: SimpleDateFormat,
    expanded: Boolean,
    payload: () -> String?,
    resending: Boolean,
    resendEnabled: Boolean,
    onToggle: () -> Unit,
    onResend: () -> Unit
) {
    val strings = LocalStrings.current
    val context = entry.context
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (context != null) {
                Text(
                    strings.logBadge(context),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (context.type == SendType.BARCODE) BarcodeAccent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (entry.success) strings.logHistorySuccessLabel else strings.logHistoryFailedLabel,
                color = if (entry.success) SuccessGreen else ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            listOfNotNull(timeFormat.format(Date(entry.timestamp)), context?.let(strings::logItemCount)).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        entry.resendOf?.let {
            Text(
                "${strings.logResentFromPrefix}${timeFormat.format(Date(it))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // if/else rather than an early return@Column: returning early out of an inline composable
        // lambda corrupts Compose's slot table on this compiler version (crash on open).
        if (context == null) {
            // Logged before type/config/payload were recorded — show the raw detail as before.
            Text(entry.detail, style = MaterialTheme.typography.bodySmall)
        } else {
            LogEntryDetails(entry, context, expanded, payload, resending, resendEnabled, onToggle, onResend)
        }
    }
}

@Composable
private fun LogEntryDetails(
    entry: SendLogEntry,
    context: SendContext,
    expanded: Boolean,
    payload: () -> String?,
    resending: Boolean,
    resendEnabled: Boolean,
    onToggle: () -> Unit,
    onResend: () -> Unit
) {
    val strings = LocalStrings.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggle, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text(if (expanded) strings.logHideDetails else strings.logShowDetails, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            if (entry.canResend) {
                OutlinedButton(
                    onClick = onResend,
                    enabled = resendEnabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    if (resending) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(strings.logResend, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (expanded) {
            val body = remember(entry) { payload() }
            if (context.type != SendType.REPLACEMENT) {
                LogDetailLine(strings.factoryCodeLabel, context.factoryCode)
                LogDetailLine(strings.rrTypeLabel, context.rrType)
            }
            LogDetailLine(strings.logUrlLabel, context.url)
            LogDetailLine(strings.logResponseLabel, entry.detail)
            if (body != null) {
                val shown = if (body.length > LOG_PAYLOAD_DISPLAY_LIMIT) body.take(LOG_PAYLOAD_DISPLAY_LIMIT) + "…" else body
                LogDetailLine(strings.logPayloadLabel, shown, monospace = true)
            }
        }
    }
}

@Composable
private fun LogDetailLine(label: String, value: String, monospace: Boolean = false) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    Text(
        value.ifBlank { "-" },
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (monospace) FontFamily.Monospace else null
    )
}
