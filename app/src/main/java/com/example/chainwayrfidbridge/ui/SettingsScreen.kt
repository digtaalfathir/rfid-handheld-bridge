package com.example.chainwayrfidbridge.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chainwayrfidbridge.ScanViewModel
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.ScanMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ScanViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(viewModel.config.value) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var testing by remember { mutableStateOf(false) }
    val antennaOptions = remember { viewModel.antennaOptions() }
    val rrTypeOptions = remember { viewModel.rrTypeOptions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
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
            SectionCard(title = "Mode") {
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
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = "Konfigurasi API") {
                LabeledField("API URL", draft.apiUrl, errors["apiUrl"]) { draft = draft.copy(apiUrl = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Reader ID", draft.readerId, errors["readerId"]) { draft = draft.copy(readerId = it) }
                Spacer(Modifier.height(8.dp))
                DropdownField("Antenna", draft.antenna, antennaOptions, errors["antenna"]) { draft = draft.copy(antenna = it) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        testing = true
                        viewModel.testConnection(draft.apiUrl) { error ->
                            testing = false
                            Toast.makeText(
                                context,
                                if (error == null) "Server dapat dijangkau" else "Gagal: $error",
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
                    Text(if (testing) "Menguji..." else "Test Connection")
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = "Register Configuration") {
                DropdownField("RR Type", draft.rrType, rrTypeOptions, errors["rrType"]) { draft = draft.copy(rrType = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Maker Name", draft.makerName, errors["makerName"]) { draft = draft.copy(makerName = it) }
                Spacer(Modifier.height(8.dp))
                LabeledField("Initial Year", draft.initialYear, errors["initialYear"]) { draft = draft.copy(initialYear = it) }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard(title = "Power (dBm)") {
                Text("${draft.power} dBm", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = draft.power.toFloat(),
                    onValueChange = { draft = draft.copy(power = it.roundToInt()) },
                    valueRange = ScanConfig.MIN_POWER.toFloat()..ScanConfig.MAX_POWER.toFloat(),
                    steps = ScanConfig.MAX_POWER - ScanConfig.MIN_POWER - 1
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        draft = viewModel.resetConfig()
                        errors = emptyMap()
                        Toast.makeText(context, "Konfigurasi direset ke default", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
                Button(
                    onClick = {
                        val result = viewModel.saveConfig(draft)
                        errors = result
                        if (result.isEmpty()) {
                            Toast.makeText(context, "Konfigurasi tersimpan", Toast.LENGTH_SHORT).show()
                            onDone()
                        } else {
                            Toast.makeText(context, "Periksa kembali input yang belum valid", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, error: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = { onSelect(it); expanded = true },
            label = { Text(label) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
