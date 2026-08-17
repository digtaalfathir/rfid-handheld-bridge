package com.example.chainwayrfidbridge.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chainwayrfidbridge.BuildConfig
import com.example.chainwayrfidbridge.ScanViewModel
import com.example.chainwayrfidbridge.data.AppLanguage
import com.example.chainwayrfidbridge.data.InputMode
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.ScanMode
import com.example.chainwayrfidbridge.data.ValidationErrorType
import com.example.chainwayrfidbridge.ui.theme.BarcodeAccent
import kotlin.math.roundToInt

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
    var testing by remember { mutableStateOf(false) }
    val powerRange = remember { viewModel.powerRange }
    val antennaOptions = remember { viewModel.antennaOptions() }
    val rrTypeOptions = remember { viewModel.rrTypeOptions() }
    val baseUrlOptions = remember { viewModel.baseUrlOptions() }
    val endpointOptions = remember { viewModel.endpointOptions() }
    val initialYearOptions = remember { viewModel.initialYearOptions() }
    val factoryCodeOptions = remember { viewModel.factoryCodeOptions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = strings.backDescription)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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
                if (draft.mode == ScanMode.REGISTER) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = draft.opname, onCheckedChange = { draft = draft.copy(opname = it) })
                        Spacer(Modifier.width(4.dp))
                        Text(strings.opnameLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.apiConfigTitle, icon = Icons.Filled.Cloud) {
                DropdownField(strings.baseUrlLabel, draft.baseUrl, baseUrlOptions, errors["baseUrl"]?.let(strings::validationMessage)) {
                    draft = draft.copy(baseUrl = it)
                }
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
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        testing = true
                        viewModel.testConnection(draft.fullApiUrl()) { error ->
                            testing = false
                            Toast.makeText(
                                context,
                                if (error == null) strings.serverReachable else "${strings.testFailedPrefix}$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (testing) strings.testing else strings.testConnection)
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = strings.registerConfigTitle, icon = Icons.Filled.Description) {
                DropdownField(strings.rrTypeLabel, draft.rrType, rrTypeOptions, errors["rrType"]?.let(strings::validationMessage)) {
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
                DropdownField(strings.factoryCodeLabel, draft.factoryCode, factoryCodeOptions, null) {
                    draft = draft.copy(factoryCode = it)
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
                    Text(strings.soundToggleLabel, style = MaterialTheme.typography.bodyMedium)
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

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        draft = viewModel.resetConfig()
                        errors = emptyMap()
                        Toast.makeText(context, strings.configResetToast, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.reset)
                }
                Button(
                    onClick = {
                        val result = viewModel.saveConfig(draft)
                        errors = result
                        if (result.isEmpty()) {
                            Toast.makeText(context, strings.configSavedToast, Toast.LENGTH_SHORT).show()
                            onDone()
                        } else {
                            Toast.makeText(context, strings.configInvalidToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.save)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "${strings.versionPrefix}${BuildConfig.VERSION_NAME}",
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
 */
@Composable
private fun DropdownField(label: String, value: String, options: List<String>, error: String?, onValueChange: (String) -> Unit) {
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
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
