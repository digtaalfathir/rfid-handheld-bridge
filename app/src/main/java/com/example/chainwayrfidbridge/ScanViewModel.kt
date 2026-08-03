package com.example.chainwayrfidbridge

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chainwayrfidbridge.data.ConfigRepository
import com.example.chainwayrfidbridge.data.DeviceType
import com.example.chainwayrfidbridge.data.ScanConfig
import com.example.chainwayrfidbridge.data.TagRecord
import com.example.chainwayrfidbridge.data.ValidationErrorType
import com.example.chainwayrfidbridge.network.ApiClient
import com.example.chainwayrfidbridge.network.UpdateClient
import com.example.chainwayrfidbridge.network.UpdateInfo
import com.example.chainwayrfidbridge.rfid.ChainwayReaderManager
import com.example.chainwayrfidbridge.rfid.RfidReaderManager
import com.example.chainwayrfidbridge.rfid.ZebraReaderManager
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
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val UI_REFRESH_INTERVAL_MS = 200L
private const val BEEP_DURATION_MS = 60
private const val BEEP_MIN_INTERVAL_MS = 90L

// Bump versionName in build.gradle to match the "vX.Y" tag whenever a new GitHub release is cut.
private const val GITHUB_REPO = "digtaalfathir/rfid-handheld-bridge"

enum class SortOption {
    LAST_SEEN_DESC,
    EPC_ASC,
    READ_COUNT_DESC,
    RSSI_DESC
}

sealed class SendStatus {
    object Idle : SendStatus()
    object Sending : SendStatus()
    data class Success(val count: Int) : SendStatus()
    data class Error(val message: String) : SendStatus()
}

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class Available(val info: UpdateInfo) : UpdateStatus()
    object Downloading : UpdateStatus()
    data class ReadyToInstall(val file: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

data class ScanUiState(
    val readerConnected: Boolean = false,
    val scanning: Boolean = false,
    val tags: List<TagRecord> = emptyList(),
    val totalReads: Int = 0,
    val lastScanTime: Long? = null,
    val sendStatus: SendStatus = SendStatus.Idle,
    val scanStartError: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.LAST_SEEN_DESC,
    val updateStatus: UpdateStatus = UpdateStatus.Idle
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val configRepo = ConfigRepository(app)

    // Decided once, at construction — chosen on the device-picker screen before this
    // ViewModel is ever created. Changing device type takes effect on next app launch.
    private val reader: RfidReaderManager = when (configRepo.loadDeviceType()) {
        DeviceType.ZEBRA -> ZebraReaderManager()
        DeviceType.CHAINWAY, null -> ChainwayReaderManager()
    }
    private val api = ApiClient()
    private val updateClient = UpdateClient()

    private val tagMap = LinkedHashMap<String, TagRecord>()
    private var sessionBaselineEpcs: Set<String> = emptySet()
    private val transitioning = AtomicBoolean(false)
    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorVolume: Int = -1
    private var lastBeepAtMs = 0L
    private var uiRefreshJob: Job? = null

    private val _config = MutableStateFlow(configRepo.load())
    val config: StateFlow<ScanConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        reader.setTriggerListener(::onTriggerPressed, ::onTriggerReleased)
        viewModelScope.launch(Dispatchers.IO) {
            val ok = reader.connect(getApplication())
            if (ok) reader.setPower(_config.value.power)
            _uiState.update { it.copy(readerConnected = ok) }
        }
        checkForUpdate()
    }

    // Held separately from updateStatus so a retry after a download Error can find the same
    // release again without re-checking GitHub (Error itself doesn't carry the UpdateInfo).
    private var pendingUpdate: UpdateInfo? = null

    /** Silent background check — never surfaces an error when there's simply nothing newer. */
    private fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = try {
                updateClient.latestRelease(GITHUB_REPO)
            } catch (e: Exception) {
                null
            } ?: return@launch
            if (info.version.isNotBlank() && info.version != BuildConfig.VERSION_NAME) {
                pendingUpdate = info
                _uiState.update { it.copy(updateStatus = UpdateStatus.Available(info)) }
            }
        }
    }

    fun downloadUpdate() {
        val info = pendingUpdate ?: return
        _uiState.update { it.copy(updateStatus = UpdateStatus.Downloading) }
        viewModelScope.launch(Dispatchers.IO) {
            val dest = File(getApplication<Application>().cacheDir, "updates/update.apk")
            val error = updateClient.download(info.downloadUrl, dest)
            _uiState.update {
                it.copy(
                    updateStatus = if (error == null) UpdateStatus.ReadyToInstall(dest) else UpdateStatus.Error(error)
                )
            }
        }
    }

    fun installUpdate() {
        val status = _uiState.value.updateStatus
        if (status !is UpdateStatus.ReadyToInstall) return
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", status.file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /** Call when the app returns to the foreground — see RfidReaderManager.onForeground(). */
    fun onAppForeground() {
        viewModelScope.launch(Dispatchers.IO) { reader.onForeground() }
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
                val error = reader.startInventory(::onTagRead)
                if (error == null) {
                    _uiState.update {
                        it.copy(scanning = true, sendStatus = SendStatus.Idle, scanStartError = null)
                    }
                    uiRefreshJob = viewModelScope.launch(Dispatchers.Default) {
                        while (isActive) {
                            delay(UI_REFRESH_INTERVAL_MS)
                            publishTags()
                        }
                    }
                } else {
                    _uiState.update { it.copy(scanStartError = error) }
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
        // Beep on every read (new or already-seen) so the operator still hears that tags are
        // being detected, not just on first discovery — but throttled to a fixed interval rather
        // than raw hardware read speed. Continuous inventory re-reads the same tag many times a
        // second, and calling startTone() that fast cuts each tone off mid-waveform instead of
        // letting it finish, which is what produced the crackly/garbled "broken speaker" sound.
        if (_config.value.soundEnabled && now - lastBeepAtMs >= BEEP_MIN_INTERVAL_MS) {
            lastBeepAtMs = now
            playBeep()
        }
    }

    /** Recreates the ToneGenerator only when the configured volume actually changes — not per
     * beep, which would reintroduce the exact scan-loop-style jank this app has already fixed
     * once before (constructing a ToneGenerator opens an audio track, not a cheap call). */
    private fun playBeep() {
        val volume = _config.value.soundVolume.coerceIn(ScanConfig.MIN_VOLUME, ScanConfig.MAX_VOLUME)
        if (toneGenerator == null || toneGeneratorVolume != volume) {
            toneGenerator?.release()
            toneGeneratorVolume = volume
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
        }
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
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
    fun saveConfig(newConfig: ScanConfig): Map<String, ValidationErrorType> {
        val errors = newConfig.validate()
        if (errors.isEmpty()) {
            configRepo.save(newConfig)
            configRepo.rememberCustomAntenna(newConfig.antenna)
            configRepo.rememberCustomRrType(newConfig.rrType)
            configRepo.rememberCustomBaseUrl(newConfig.baseUrl)
            configRepo.rememberCustomInitialYear(newConfig.initialYear)
            _config.value = newConfig
            viewModelScope.launch(Dispatchers.IO) { reader.setPower(newConfig.power) }
        }
        return errors
    }

    fun antennaOptions(): List<String> = configRepo.antennaOptions()

    fun rrTypeOptions(): List<String> = configRepo.rrTypeOptions()

    fun baseUrlOptions(): List<String> = configRepo.baseUrlOptions()

    fun initialYearOptions(): List<String> = configRepo.initialYearOptions()

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
        toneGenerator?.release()
    }
}
