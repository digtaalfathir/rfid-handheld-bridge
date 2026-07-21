package com.example.chainwayrfidbridge

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chainwayrfidbridge.data.ConfigRepository
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.TagRecord
import com.example.chainwayrfidbridge.network.ApiClient
import com.example.chainwayrfidbridge.rfid.RfidReaderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val UI_REFRESH_INTERVAL_MS = 200L

enum class SortOption(val label: String) {
    LAST_SEEN_DESC("Terbaru"),
    EPC_ASC("EPC A-Z"),
    READ_COUNT_DESC("Read Count"),
    RSSI_DESC("RSSI")
}

sealed class SendStatus {
    object Idle : SendStatus()
    object Sending : SendStatus()
    data class Success(val count: Int) : SendStatus()
    data class Error(val message: String) : SendStatus()
}

data class ScanUiState(
    val readerConnected: Boolean = false,
    val scanning: Boolean = false,
    val tags: List<TagRecord> = emptyList(),
    val totalReads: Int = 0,
    val lastScanTime: Long? = null,
    val sendStatus: SendStatus = SendStatus.Idle,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.LAST_SEEN_DESC
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val configRepo = ConfigRepository(app)
    private val reader = RfidReaderManager()
    private val api = ApiClient()

    private val tagMap = LinkedHashMap<String, TagRecord>()
    private var sessionBaselineEpcs: Set<String> = emptySet()
    private val transitioning = AtomicBoolean(false)
    private val toneGenerator by lazy { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }
    private var uiRefreshJob: Job? = null

    private val _config = MutableStateFlow(configRepo.load())
    val config: StateFlow<ScanConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = reader.connect(getApplication())
            if (ok) reader.setPower(_config.value.power)
            _uiState.update { it.copy(readerConnected = ok) }
        }
    }

    fun toggleScan() {
        if (!transitioning.compareAndSet(false, true)) return
        if (_uiState.value.scanning) stopScanAndSend() else startScan()
    }

    /** Physical trigger, pressed: hold-to-scan. Always restarts fresh when tags are already present. */
    fun onTriggerPressed() {
        val s = _uiState.value
        if (s.scanning) return
        if (s.tags.isEmpty()) toggleScan() else startNewScan()
    }

    /** Physical trigger, released: stop (mirrors the on-screen Stop Scan button). */
    fun onTriggerReleased() {
        if (_uiState.value.scanning) toggleScan()
    }

    /** Clears previously collected tags, then starts a scan from scratch. */
    fun startNewScan() {
        if (!transitioning.compareAndSet(false, true)) return
        if (!reader.isConnected()) {
            transitioning.set(false)
            return
        }
        synchronized(tagMap) { tagMap.clear() }
        sessionBaselineEpcs = emptySet()
        publishTags()
        beginInventory()
    }

    private fun startScan() {
        if (!reader.isConnected()) {
            transitioning.set(false)
            return
        }
        sessionBaselineEpcs = synchronized(tagMap) { tagMap.keys.toSet() }
        beginInventory()
    }

    private fun beginInventory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val started = reader.startInventory(::onTagRead)
                if (started) {
                    _uiState.update { it.copy(scanning = true, sendStatus = SendStatus.Idle) }
                    uiRefreshJob = viewModelScope.launch(Dispatchers.Default) {
                        while (isActive) {
                            delay(UI_REFRESH_INTERVAL_MS)
                            publishTags()
                        }
                    }
                }
            } finally {
                transitioning.set(false)
            }
        }
    }

    /** Runs on the reader's poll thread at hardware read speed — keep this cheap, no sorting/UI work here. */
    private fun onTagRead(epc: String, rssi: String) {
        val now = System.currentTimeMillis()
        synchronized(tagMap) {
            val existing = tagMap[epc]
            tagMap[epc] = if (existing == null) {
                TagRecord(epc, now, now, 1, _config.value.antenna, rssi, isNew = epc !in sessionBaselineEpcs)
            } else {
                existing.copy(lastSeen = now, readCount = existing.readCount + 1, rssi = rssi)
            }
        }
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
    }

    private fun stopScanAndSend() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = try {
                uiRefreshJob?.cancel()
                uiRefreshJob = null
                reader.stopInventory()
                synchronized(tagMap) { tagMap.values.toList() }
            } finally {
                transitioning.set(false)
            }
            publishTags()
            _uiState.update {
                it.copy(
                    scanning = false,
                    lastScanTime = System.currentTimeMillis(),
                    sendStatus = if (snapshot.isEmpty()) SendStatus.Idle else SendStatus.Sending
                )
            }
            if (snapshot.isEmpty()) return@launch
            sendSnapshot(snapshot)
        }
    }

    /** Re-sends the currently held tags; only meaningful after a failed send. */
    fun retrySend() {
        if (_uiState.value.sendStatus !is SendStatus.Error) return
        val snapshot = synchronized(tagMap) { tagMap.values.toList() }
        if (snapshot.isEmpty()) return
        _uiState.update { it.copy(sendStatus = SendStatus.Sending) }
        viewModelScope.launch(Dispatchers.IO) { sendSnapshot(snapshot) }
    }

    private suspend fun sendSnapshot(snapshot: List<TagRecord>) {
        val error = api.sendTags(_config.value, snapshot)
        _uiState.update {
            it.copy(sendStatus = if (error == null) SendStatus.Success(snapshot.size) else SendStatus.Error(error))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        publishTags()
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        publishTags()
    }

    private fun publishTags() {
        val snapshot = synchronized(tagMap) { tagMap.values.toList() }
        _uiState.update {
            it.copy(
                tags = applySortAndFilter(snapshot, it.sortOption, it.searchQuery),
                totalReads = snapshot.sumOf { t -> t.readCount }
            )
        }
    }

    private fun applySortAndFilter(
        all: List<TagRecord>,
        sort: SortOption,
        query: String
    ): List<TagRecord> {
        var list = all
        if (query.isNotBlank()) list = list.filter { it.epc.contains(query, ignoreCase = true) }
        return when (sort) {
            SortOption.LAST_SEEN_DESC -> list.sortedByDescending { it.lastSeen }
            SortOption.EPC_ASC -> list.sortedBy { it.epc }
            SortOption.READ_COUNT_DESC -> list.sortedByDescending { it.readCount }
            SortOption.RSSI_DESC -> list.sortedByDescending { it.rssi.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
        }
    }

    fun setPower(dbm: Int) {
        _config.update { it.copy(power = dbm) }
        configRepo.save(_config.value)
        viewModelScope.launch(Dispatchers.IO) { reader.setPower(dbm) }
    }

    /** Returns field->error map; empty means the config was valid and got saved. */
    fun saveConfig(newConfig: ScanConfig): Map<String, String> {
        val errors = newConfig.validate()
        if (errors.isEmpty()) {
            configRepo.save(newConfig)
            configRepo.rememberCustomAntenna(newConfig.antenna)
            configRepo.rememberCustomRrType(newConfig.rrType)
            _config.value = newConfig
            viewModelScope.launch(Dispatchers.IO) { reader.setPower(newConfig.power) }
        }
        return errors
    }

    fun antennaOptions(): List<String> = configRepo.antennaOptions()

    fun rrTypeOptions(): List<String> = configRepo.rrTypeOptions()

    fun resetConfig(): ScanConfig {
        val defaults = configRepo.reset()
        _config.value = defaults
        return defaults
    }

    fun testConnection(apiUrl: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = api.testConnection(apiUrl)
            withContext(Dispatchers.Main) { onResult(error) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        reader.release()
        toneGenerator.release()
    }
}
