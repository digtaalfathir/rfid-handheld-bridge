package com.stechoq.wms

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.stechoq.wms.barcode.BarcodeReaderManager
import com.stechoq.wms.barcode.ChainwayBarcodeManager
import com.stechoq.wms.barcode.ZebraBarcodeManager
import com.stechoq.wms.data.BarcodeScanRecord
import com.stechoq.wms.data.BarcodeSendStatus
import com.stechoq.wms.data.ConfigRepository
import com.stechoq.wms.data.DeviceType
import com.stechoq.wms.data.FactoryCodeOption
import com.stechoq.wms.data.InputMode
import com.stechoq.wms.data.ReplacementReason
import com.stechoq.wms.data.ScanConfig
import com.stechoq.wms.data.ScanMode
import com.stechoq.wms.data.SendContext
import com.stechoq.wms.data.SendLogEntry
import com.stechoq.wms.data.SendType
import com.stechoq.wms.data.TagQuality
import com.stechoq.wms.data.TagRecord
import com.stechoq.wms.data.ValidationErrorType
import com.stechoq.wms.network.ApiClient
import com.stechoq.wms.network.OutgoingRequest
import com.stechoq.wms.network.UpdateClient
import com.stechoq.wms.network.UpdateInfo
import com.stechoq.wms.network.markAsResend
import com.stechoq.wms.rfid.ChainwayReaderManager
import com.stechoq.wms.rfid.RfidReaderManager
import com.stechoq.wms.rfid.ZebraReaderManager
import com.stechoq.wms.service.ScanStateBus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val UI_REFRESH_INTERVAL_MS = 200L
private const val BEEP_DURATION_MS = 60
private const val BEEP_MIN_INTERVAL_MS = 90L
private const val BACKGROUND_REASSERT_INTERVAL_MS = 3000L
private const val UPDATE_CHECK_MIN_INTERVAL_MS = 5 * 60 * 1000L

// Bump versionName in build.gradle to match the "vX.Y" tag whenever a new GitHub release is cut.
private const val GITHUB_REPO = "digtaalfathir/rfid-handheld-bridge"

enum class SortOption {
    LAST_SEEN_DESC,
    EPC_ASC,
    READ_COUNT_DESC,
    QUALITY_DESC
}

sealed class SendStatus {
    object Idle : SendStatus()
    object Sending : SendStatus()
    data class Success(val count: Int) : SendStatus()
    // Raw, unlocalized detail (HTTP status + body, or an exception message) — the UI layer turns
    // this into a display message via formatSendError(), same as every other localized string
    // here coming from AppStrings rather than the ViewModel.
    // [unsentCount]: how many tags that failed send carried — shown in the error banner.
    data class Error(val message: String, val unsentCount: Int) : SendStatus()
}

/** State for the "replace a damaged tag" modal — shown mid-scan (Register mode + Replacement
 * enabled) the instant a single tag is detected. [newRfid] is that scanned tag; [submitting]/
 * [error] track the in-flight POST once the operator fills in the rest and taps Submit. */
data class ReplacementPrompt(
    val newRfid: String,
    val submitting: Boolean = false,
    val error: String? = null
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class Available(val info: UpdateInfo) : UpdateStatus()
    object Downloading : UpdateStatus()
    data class ReadyToInstall(val file: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/** Status of the RR Type / Factory Code fetch shown under Settings' Base URL field. Each state
 * carries the Base URL it belongs to. */
sealed class DropdownFetchState {
    object Idle : DropdownFetchState()
    data class Loading(val baseUrl: String) : DropdownFetchState()
    data class Success(
        val baseUrl: String,
        val rrTypes: List<String>,
        val factoryCodes: List<FactoryCodeOption>
    ) : DropdownFetchState()
    // hasStaleCache: the dropdowns still hold an earlier successful fetch, possibly from a
    // different server — worth flagging, since nothing on screen otherwise says where it came from.
    data class Failed(val baseUrl: String, val hasStaleCache: Boolean) : DropdownFetchState()
}

data class ScanUiState(
    val readerConnected: Boolean = false,
    val scanning: Boolean = false,
    val tags: List<TagRecord> = emptyList(),
    val totalReads: Int = 0,
    // Every collected tag, regardless of the search filter — [tags] is what's visible after it.
    val uniqueCount: Int = 0,
    val lastScanTime: Long? = null,
    val sendStatus: SendStatus = SendStatus.Idle,
    val scanStartError: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.LAST_SEEN_DESC,
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val barcodeScans: List<BarcodeScanRecord> = emptyList(),
    val replacementPrompt: ReplacementPrompt? = null,
    // Non-null while the "unsent tags" confirmation is showing.
    val unsentTagsPrompt: UnsentTagsPrompt? = null
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val configRepo = ConfigRepository(app)

    // Decided once, at construction — chosen on the device-picker screen before this
    // ViewModel is ever created. Changing device type takes effect on next app launch.
    private val reader: RfidReaderManager = when (configRepo.loadDeviceType()) {
        DeviceType.ZEBRA -> ZebraReaderManager()
        DeviceType.CHAINWAY, null -> ChainwayReaderManager()
    }
    private val barcodeManager: BarcodeReaderManager = when (configRepo.loadDeviceType()) {
        DeviceType.ZEBRA -> ZebraBarcodeManager()
        DeviceType.CHAINWAY, null -> ChainwayBarcodeManager()
    }
    private val api = ApiClient()
    private val updateClient = UpdateClient()

    private val tagMap = LinkedHashMap<String, TagRecord>()
    private var sessionBaselineEpcs: Set<String> = emptySet()
    private val transitioning = AtomicBoolean(false)
    // Guards the replacement-mode auto-stop so it fires at most once per scan — onTagRead can run
    // several times before stopInventory() actually takes effect on the hardware.
    private val replacementPromptShown = AtomicBoolean(false)
    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorVolume: Int = -1
    private var lastBeepAtMs = 0L
    private var uiRefreshJob: Job? = null

    private val _config = MutableStateFlow(configRepo.load())
    val config: StateFlow<ScanConfig> = _config.asStateFlow()

    private val _dropdownFetch = MutableStateFlow<DropdownFetchState>(DropdownFetchState.Idle)
    val dropdownFetch: StateFlow<DropdownFetchState> = _dropdownFetch.asStateFlow()
    private var dropdownFetchJob: Job? = null

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        reader.setTriggerListener(::onTriggerPressed, ::onTriggerReleased)
        barcodeManager.setResultListener(::onBarcodeScanned)
        viewModelScope.launch(Dispatchers.IO) {
            val ok = reader.connect(getApplication())
            if (ok) reader.setPower(_config.value.power)
            reader.setInputMode(_config.value.inputMode)
            barcodeManager.connect(getApplication())
            barcodeManager.setActive(_config.value.inputMode == InputMode.BARCODE)
            _uiState.update { it.copy(readerConnected = ok) }
        }
        checkForUpdate()
    }

    // Held separately from updateStatus so a retry after a download Error can find the same
    // release again without re-checking GitHub (Error itself doesn't carry the UpdateInfo).
    private var pendingUpdate: UpdateInfo? = null
    private var lastUpdateCheckAtMs = 0L

    /** Silent background check — never surfaces an error when there's simply nothing newer. Runs
     * on cold start (from init) and every time the app is reopened (from onAppForeground), since
     * a ViewModel surviving a simple background/foreground cycle wouldn't otherwise re-check on
     * what the operator experiences as "opening the app" again. Debounced so rapidly switching
     * away and back doesn't hit GitHub's API on every single resume. */
    private fun checkForUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastUpdateCheckAtMs < UPDATE_CHECK_MIN_INTERVAL_MS) return
        lastUpdateCheckAtMs = now
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

    private var backgroundReassertJob: Job? = null

    /** Call when the app returns to the foreground — see RfidReaderManager.onForeground(). */
    fun onAppForeground() {
        backgroundReassertJob?.cancel()
        backgroundReassertJob = null
        viewModelScope.launch(Dispatchers.IO) { reader.onForeground() }
        checkForUpdate()
    }

    /** Call when the app leaves the foreground — see RfidReaderManager.onBackground(). A single
     * reassertion right at this transition isn't enough: DataWedge re-claims the trigger not
     * only when we lose focus but again whenever a "real" app with its own DataWedge profile
     * (e.g. Chrome) comes to foreground — the launcher/home screen apparently doesn't trigger
     * that, which is why this only ever showed up when opening another app, not going home. Keep
     * re-asserting on a short interval for as long as we're backgrounded with background
     * scanning on, so we reclaim the trigger again shortly after DataWedge takes it. */
    fun onAppBackground() {
        viewModelScope.launch(Dispatchers.IO) { reader.onBackground() }
        backgroundReassertJob?.cancel()
        if (!_config.value.backgroundScanEnabled) return
        backgroundReassertJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(BACKGROUND_REASSERT_INTERVAL_MS)
                reader.onBackground()
            }
        }
    }

    fun toggleScan() {
        if (!transitioning.compareAndSet(false, true)) return
        if (_uiState.value.scanning) stopScanAndSend() else startScan()
    }

    /** Physical trigger, pressed: hold-to-scan (RFID) or arm the barcode decoder (Barcode) —
     * whichever manager owns the trigger for the currently selected InputMode. Restarts fresh
     * when tags are already present, for RFID — via [requestNewScan], so unsent tags aren't
     * silently dropped. */
    fun onTriggerPressed() {
        if (!canScanNow()) return
        if (_config.value.inputMode == InputMode.BARCODE) {
            barcodeManager.startScan()
            return
        }
        val s = _uiState.value
        if (s.scanning) return
        if (collectedTagCount() == 0) toggleScan() else requestNewScan(fromTrigger = true)
    }

    // Not uiState.tags — that's only what the search filter currently shows.
    private fun collectedTagCount(): Int = synchronized(tagMap) { tagMap.size }

    /** New Scan button / trigger with tags present: starts fresh unless the tags on screen were
     * never delivered — then the operator decides (send first, or discard) via the confirmation
     * dialog. While a send is still in flight the request is ignored until its result is known. */
    fun requestNewScan(fromTrigger: Boolean = false) {
        val s = _uiState.value
        val count = collectedTagCount()
        when (newScanDecision(s.sendStatus, count)) {
            NewScanDecision.START -> startNewScan()
            NewScanDecision.IGNORE -> Unit
            // Backgrounded (Background Scanning on): there's no screen to show the dialog on, so
            // the trigger does nothing — the bubble already shows red for the failed send, and
            // the operator gets the choice once they open the app and press again.
            NewScanDecision.CONFIRM_DISCARD -> if (isAppVisible()) {
                _uiState.update { it.copy(unsentTagsPrompt = UnsentTagsPrompt(count, fromTrigger)) }
            }
        }
    }

    /** "Send first" in the unsent-tags dialog — same as the Retry button; tags stay on screen. */
    fun sendUnsentTagsFirst() {
        dismissUnsentTagsPrompt()
        retrySend()
    }

    /** "Discard & new scan" in the unsent-tags dialog — the operator's explicit choice. See
     * [UnsentTagsPrompt.fromTrigger] for why the trigger case only clears. */
    fun discardUnsentTags() {
        val prompt = _uiState.value.unsentTagsPrompt ?: return
        dismissUnsentTagsPrompt()
        if (!prompt.fromTrigger) {
            startNewScan()
            return
        }
        clearTags()
    }

    /** Every path that empties the tag list goes through here, so a failed-send banner (and its
     * Retry, which has nothing left to resend) can never linger over an empty list — e.g. when
     * New Scan cleared the tags but the reader then failed to start. */
    private fun clearTags() {
        synchronized(tagMap) { tagMap.clear() }
        sessionBaselineEpcs = emptySet()
        publishTags()
        if (_uiState.value.sendStatus is SendStatus.Error) {
            _uiState.update { it.copy(sendStatus = SendStatus.Idle) }
            ScanStateBus.update(ScanStateBus.BubbleState.IDLE, 0)
        }
    }

    fun dismissUnsentTagsPrompt() {
        _uiState.update { it.copy(unsentTagsPrompt = null) }
    }

    /** Physical trigger, released: stop (mirrors the on-screen Stop Scan button in RFID mode). */
    fun onTriggerReleased() {
        if (!canScanNow()) return
        if (_config.value.inputMode == InputMode.BARCODE) {
            barcodeManager.stopScan()
            return
        }
        if (_uiState.value.scanning) toggleScan()
    }

    /** A single decoded barcode — sent immediately with the same payload shape RFID uses, one
     * request per code rather than batched, since that's how barcode scanning is naturally used
     * (point, scan, confirm, move on) unlike RFID's bulk-inventory-then-send flow. */
    private fun onBarcodeScanned(code: String) {
        val record = BarcodeScanRecord(code, System.currentTimeMillis(), BarcodeSendStatus.SENDING)
        _uiState.update { it.copy(barcodeScans = listOf(record) + it.barcodeScans) }
        if (_config.value.soundEnabled) playBeep()
        viewModelScope.launch(Dispatchers.IO) {
            val config = _config.value
            val request = api.codesRequest(config, mapOf(code to TagQuality.STRONG.wireValue))
            val error = api.post(request)
            val newStatus = if (error == null) BarcodeSendStatus.SENT else BarcodeSendStatus.FAILED
            logSend(SendContext.of(config, SendType.BARCODE, request.url, 1), request, error, "OK ($code)")
            _uiState.update { state ->
                state.copy(barcodeScans = state.barcodeScans.map {
                    if (it === record) it.copy(status = newStatus) else it
                })
            }
        }
    }

    /** Background Scanning off + app not visible = ignore the trigger entirely. Some of these
     * handhelds route the trigger key straight to the app regardless of window focus, so this
     * check (not Activity focus) is what actually enforces the setting. */
    private fun canScanNow(): Boolean {
        if (_config.value.backgroundScanEnabled) return true
        return isAppVisible()
    }

    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    /** Clears previously collected tags, then starts a scan from scratch. */
    fun startNewScan() {
        if (!transitioning.compareAndSet(false, true)) return
        if (!reader.isConnected()) {
            transitioning.set(false)
            return
        }
        clearTags()
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
        replacementPromptShown.set(false)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val error = reader.startInventory(::onTagRead)
                if (error == null) {
                    _uiState.update {
                        it.copy(scanning = true, sendStatus = SendStatus.Idle, scanStartError = null)
                    }
                    publishTags()
                    ScanStateBus.update(ScanStateBus.BubbleState.SCANNING, synchronized(tagMap) { tagMap.size })
                    uiRefreshJob = viewModelScope.launch(Dispatchers.Default) {
                        while (isActive) {
                            delay(UI_REFRESH_INTERVAL_MS)
                            publishTags()
                            // Live count on the floating bubble while backgrounded, same cadence as the in-app tag list.
                            ScanStateBus.update(ScanStateBus.BubbleState.SCANNING, synchronized(tagMap) { tagMap.size })
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
        val tagCount = synchronized(tagMap) {
            val existing = tagMap[epc]
            tagMap[epc] = if (existing == null) {
                TagRecord(epc, now, now, 1, _config.value.antenna, rssi, isNew = epc !in sessionBaselineEpcs)
            } else {
                existing.copy(lastSeen = now, readCount = existing.readCount + 1, rssi = rssi)
            }
            tagMap.size
        }
        // Replacement mode wants exactly one tag — the physical replacement being registered —
        // not a batch, so the instant that first (and, until Submit/Cancel, only) tag shows up,
        // stop immediately rather than let inventory keep running. Guarded so this only fires once
        // per scan even though this callback keeps firing on the poll thread until the stop lands.
        if (_config.value.mode == ScanMode.REGISTER && _config.value.replacementModeEnabled &&
            tagCount == 1 && replacementPromptShown.compareAndSet(false, true)
        ) {
            stopForReplacementPrompt(epc)
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
            if (snapshot.isEmpty()) {
                ScanStateBus.update(ScanStateBus.BubbleState.IDLE, 0)
                return@launch
            }
            saveCsvBackup(snapshot)
            sendSnapshot(snapshot)
        }
    }

    /** Replacement mode's auto-stop: reached exactly one tag mid-scan, so stop inventory
     * immediately and show the replacement modal instead of continuing to batch-scan or
     * auto-sending anything — submitReplacement() below posts to a completely different endpoint
     * than the normal tag batch. */
    private fun stopForReplacementPrompt(epc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uiRefreshJob?.cancel()
                uiRefreshJob = null
                reader.stopInventory()
            } finally {
                transitioning.set(false)
            }
            publishTags()
            _uiState.update {
                it.copy(
                    scanning = false,
                    lastScanTime = System.currentTimeMillis(),
                    replacementPrompt = ReplacementPrompt(epc)
                )
            }
            ScanStateBus.update(ScanStateBus.BubbleState.IDLE, 0)
        }
    }

    /** Submits the replacement; [onResult] lets the caller show a one-shot success toast (same
     * callback shape the Settings screen's old connection test used) — failure is instead reflected
     * inline via the prompt's own [ReplacementPrompt.error], so the modal stays open with the
     * fields intact for a retry rather than needing the tag re-scanned. */
    fun submitReplacement(oldRfid: String, reason: ReplacementReason, onResult: (Boolean) -> Unit) {
        val prompt = _uiState.value.replacementPrompt ?: return
        // The dialog already disables Submit in this case; this is the backstop, so factory_id = 0
        // can never reach the server even from a config saved before Factory Code was validated.
        if (_config.value.factoryId <= 0) return
        _uiState.update { it.copy(replacementPrompt = prompt.copy(submitting = true, error = null)) }
        viewModelScope.launch(Dispatchers.IO) {
            val config = _config.value
            val request = api.replacementRequest(config, oldRfid, prompt.newRfid, reason.wireValue)
            val error = api.post(request)
            logSend(
                SendContext.of(config, SendType.REPLACEMENT, request.url, 1), request, error,
                "OK (replace ${prompt.newRfid})"
            )
            if (error == null) clearTags()
            _uiState.update {
                it.copy(replacementPrompt = if (error == null) null else prompt.copy(submitting = false, error = error))
            }
            withContext(Dispatchers.Main) { onResult(error == null) }
        }
    }

    /** Discards the scanned tag and closes the modal without submitting anything. */
    fun cancelReplacement() {
        _uiState.update { it.copy(replacementPrompt = null) }
        clearTags()
    }

    /** Local backup, independent of whether the API send below succeeds — best-effort, never
     * blocks or fails the actual scan-send flow if storage is unavailable for some reason. */
    private fun saveCsvBackup(tags: List<TagRecord>) {
        if (!_config.value.localCsvEnabled) return
        try {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "rfid_stc")
            dir.mkdirs()
            val filename = "scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            File(dir, filename).bufferedWriter().use { writer ->
                writer.write("epc,first_seen,last_seen,read_count,antenna,rssi,is_new\n")
                tags.forEach { t ->
                    writer.write("${t.epc},${t.firstSeen},${t.lastSeen},${t.readCount},${t.antenna},${t.rssi},${t.isNew}\n")
                }
            }
        } catch (e: Exception) {
            // best-effort — local backup failure shouldn't block sending to the server
        }
    }

    /** Re-sends the currently held tags; only meaningful after a failed send. */
    fun retrySend() {
        if (_uiState.value.sendStatus !is SendStatus.Error) return
        val snapshot = synchronized(tagMap) { tagMap.values.toList() }
        if (snapshot.isEmpty()) {
            // Nothing left to resend — drop the stale error instead of a Retry that does nothing.
            clearTags()
            return
        }
        _uiState.update { it.copy(sendStatus = SendStatus.Sending) }
        viewModelScope.launch(Dispatchers.IO) { sendSnapshot(snapshot) }
    }

    private suspend fun sendSnapshot(snapshot: List<TagRecord>) {
        val config = _config.value
        val request = api.tagsRequest(config, snapshot)
        val error = api.post(request)
        _uiState.update {
            it.copy(
                sendStatus = if (error == null) SendStatus.Success(snapshot.size) else SendStatus.Error(error, snapshot.size)
            )
        }
        logSend(
            SendContext.of(config, SendType.RFID_BATCH, request.url, snapshot.size), request, error,
            "OK (${snapshot.size} tag)"
        )
        ScanStateBus.update(
            if (error == null) ScanStateBus.BubbleState.SUCCESS else ScanStateBus.BubbleState.ERROR,
            snapshot.size
        )
    }

    /** Every send (scan batch, barcode, replacement, or a resend from Log History) is logged with
     * its type, the config it went out under, and the exact body — so it can be shown and resent
     * later regardless of what Settings say by then. */
    private fun logSend(
        context: SendContext,
        request: OutgoingRequest,
        error: String?,
        okDetail: String,
        resendOf: Long? = null
    ) {
        configRepo.appendSendLog(
            SendLogEntry(
                timestamp = System.currentTimeMillis(),
                success = error == null,
                detail = error ?: okDetail,
                id = UUID.randomUUID().toString(),
                context = context,
                resendOf = resendOf
            ),
            payload = request.body
        )
    }

    /** Resends a Log History entry exactly as it went out — its stored body (plus resend markers,
     * see [markAsResend]) to its stored URL, not rebuilt from the current Settings. The result is
     * logged as a new entry pointing back at [entry]. [onResult] reports success. */
    fun resendLogEntry(entry: SendLogEntry, onResult: (Boolean) -> Unit) {
        val context = entry.context
        val id = entry.id
        if (!entry.canResend || context == null || id == null) return
        viewModelScope.launch(Dispatchers.IO) {
            val body = configRepo.sendLogPayload(id)
            val success = if (body == null) {
                false
            } else {
                val request = OutgoingRequest(context.url, markAsResend(body, entry.timestamp))
                val error = api.post(request)
                logSend(context, request, error, "OK (resend)", resendOf = entry.timestamp)
                error == null
            }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun sendLogPayload(entry: SendLogEntry): String? = entry.id?.let(configRepo::sendLogPayload)

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
                totalReads = snapshot.sumOf { t -> t.readCount },
                uniqueCount = snapshot.size
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
            SortOption.QUALITY_DESC -> list.sortedByDescending { TagQuality.from(it.rssi, it.readCount) }
        }
    }

    /** Native valid range for the connected backend — Chainway 1-30 dBm, Zebra 0-300 (its own
     * capability table units, shown/edited directly rather than remapped onto a shared scale). */
    val powerRange: IntRange get() = reader.powerRange

    fun setPower(level: Int) {
        _config.update { it.copy(power = level) }
        configRepo.save(_config.value)
        viewModelScope.launch(Dispatchers.IO) { reader.setPower(level) }
    }

    /** Returns field->error map; empty means the config was valid and got saved. */
    fun saveConfig(rawConfig: ScanConfig): Map<String, ValidationErrorType> {
        // Replacement always implies opname — enforced here too, not just in the Settings UI's
        // checkbox lock, so a config saved from anywhere can never persist the two disagreeing.
        val newConfig = if (rawConfig.replacementModeEnabled) rawConfig.copy(opname = true) else rawConfig
        val errors = newConfig.validate(reader.powerRange)
        if (errors.isEmpty()) {
            val inputModeChanged = newConfig.inputMode != _config.value.inputMode
            configRepo.save(newConfig)
            configRepo.rememberCustomAntenna(newConfig.antenna)
            configRepo.rememberCustomBaseUrl(newConfig.baseUrl)
            configRepo.rememberCustomEndpoint(newConfig.endpoint)
            configRepo.rememberCustomInitialYear(newConfig.initialYear)
            _config.value = newConfig
            viewModelScope.launch(Dispatchers.IO) {
                reader.setPower(newConfig.power)
                if (inputModeChanged) {
                    reader.setInputMode(newConfig.inputMode)
                    barcodeManager.setActive(newConfig.inputMode == InputMode.BARCODE)
                }
            }
        }
        return errors
    }

    fun antennaOptions(): List<String> = configRepo.antennaOptions()

    fun rrTypeOptions(): List<String> = configRepo.rrTypeOptions()

    fun baseUrlOptions(): List<String> = configRepo.baseUrlOptions()

    fun endpointOptions(): List<String> = configRepo.endpointOptions()

    fun initialYearOptions(): List<String> = configRepo.initialYearOptions()

    fun factoryCodeOptions(): List<FactoryCodeOption> = configRepo.factoryCodeOptions()

    /** Last 10 send attempts (RFID batch or barcode), newest first — the full/raw detail behind
     * whatever generalized message the Scan screen showed at the time. */
    fun sendLog(): List<SendLogEntry> = configRepo.sendLog()

    /** Fetches RR Type and Factory Code lists from [baseUrl], cancelling any fetch still in flight
     * so a slow response from a previous Base URL can never land on top of this one. The cache is
     * only overwritten when both lists succeed — a half-success would leave the two dropdowns
     * holding data from different servers. On failure the old cache is kept as-is. */
    fun fetchDropdownData(baseUrl: String) {
        dropdownFetchJob?.cancel()
        _dropdownFetch.value = DropdownFetchState.Loading(baseUrl)
        dropdownFetchJob = viewModelScope.launch {
            // Launched on Main, so cancel() (also called from Main) either lands before
            // withContext resumes — which then throws instead of returning — or after this whole
            // block already finished; a stale result can't slip in between.
            val (rrTypes, factoryCodes) = withContext(Dispatchers.IO) {
                api.fetchRrTypes(baseUrl) to api.fetchFactoryCodes(baseUrl)
            }
            _dropdownFetch.value = if (rrTypes != null && factoryCodes != null) {
                configRepo.saveRrTypeOptions(rrTypes)
                configRepo.saveFactoryCodeOptions(factoryCodes)
                DropdownFetchState.Success(baseUrl, rrTypes, factoryCodes)
            } else {
                val hasCache = configRepo.rrTypeOptions().isNotEmpty() || configRepo.factoryCodeOptions().isNotEmpty()
                DropdownFetchState.Failed(baseUrl, hasStaleCache = hasCache)
            }
        }
    }

    fun cancelDropdownFetch() {
        dropdownFetchJob?.cancel()
        dropdownFetchJob = null
        _dropdownFetch.value = DropdownFetchState.Idle
    }

    fun resetConfig(): ScanConfig {
        val defaults = configRepo.reset()
        _config.value = defaults
        return defaults
    }

    override fun onCleared() {
        super.onCleared()
        reader.release()
        barcodeManager.release()
        toneGenerator?.release()
    }
}
