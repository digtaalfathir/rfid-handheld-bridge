package com.example.chainwayrfidbridge.rfid

import android.content.Context
import android.content.Intent
import com.example.chainwayrfidbridge.data.InputMode
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE
import com.zebra.rfid.api3.INVENTORY_STATE
import com.zebra.rfid.api3.OperationFailureException
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.ReaderDevice
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.SESSION
import com.zebra.rfid.api3.SL_FLAG
import com.zebra.rfid.api3.START_TRIGGER_TYPE
import com.zebra.rfid.api3.STATUS_EVENT_TYPE
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE
import com.zebra.rfid.api3.TriggerInfo

/**
 * Zebra RFID API3 SDK backend (built-in handheld reader). Verified method-by-method against
 * Zebra's own SDKSample source (RFIDHandler.java) and the real class bytecode in
 * API3_LIB-release-2.0.1.29.aar — not guessed.
 *
 * Tag reads: the SDK pushes an eventReadNotify() with no data attached (setAttachTagDataWithReadEvent
 * is left false, matching Zebra's own sample), so each notify pulls the buffered tags via
 * Actions.getReadTags().
 *
 * Physical trigger: comes through the SDK's own HANDHELD_TRIGGER_EVENT status event, not a raw
 * Android key event (unlike Chainway, which needed scancode sniffing at the Activity level).
 * This class only forwards press/release via setTriggerListener — same as Chainway, ScanViewModel
 * decides what a trigger press actually does (fresh scan vs continue vs stop).
 */
class ZebraReaderManager : RfidReaderManager, Readers.RFIDReaderEventHandler {

    private var readers: Readers? = null
    private var reader: RFIDReader? = null
    private var appContext: Context? = null
    private var scanning = false
    private var currentInputMode = InputMode.RFID
    private var maxPowerIndex = 0
    private var onTagCallback: ((epc: String, rssi: String) -> Unit)? = null
    private var onTriggerPressed: (() -> Unit)? = null
    private var onTriggerReleased: (() -> Unit)? = null

    private val eventListener = object : RfidEventsListener {
        override fun eventReadNotify(e: RfidReadEvents?) {
            val r = reader ?: return
            val cb = onTagCallback ?: return
            val tags = try {
                r.Actions.getReadTags(100)
            } catch (ex: Exception) {
                null
            } ?: return
            for (tag in tags) {
                val epc = tag.getTagID()
                if (!epc.isNullOrEmpty()) cb(epc, tag.getPeakRSSI().toString())
            }
        }

        override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents?) {
            val data = rfidStatusEvents?.StatusEventData ?: return
            if (data.getStatusEventType() != STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) return
            when (data.HandheldTriggerEventData.getHandheldEvent()) {
                HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED -> onTriggerPressed?.invoke()
                HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED -> onTriggerReleased?.invoke()
                else -> {}
            }
        }
    }

    override fun connect(context: Context): Boolean {
        appContext = context.applicationContext
        setDataWedgeScannerEnabled(context, false)
        return try {
            val r = readers ?: Readers(context, ENUM_TRANSPORT.SERVICE_SERIAL).also { readers = it }
            Readers.attach(this)
            val list: ArrayList<ReaderDevice> = r.GetAvailableRFIDReaderList() ?: return false
            if (list.isEmpty()) return false
            val rfidReader = list[0].getRFIDReader() ?: return false
            reader = rfidReader
            if (!rfidReader.isConnected()) rfidReader.connect()
            configureReader(rfidReader)
            rfidReader.isConnected()
        } catch (e: Exception) {
            reader = null
            false
        }
    }

    // Root-cause fix for the trigger firing both RFID and barcode scans at once in RFID mode:
    // DataWedge (Zebra's barcode engine) reclaims the physical trigger whenever it feels like it,
    // no matter what ENUM_TRIGGER_MODE we've set — reasserting our own mode on resume only ever
    // papered over it. While in RFID mode this app has no use for DataWedge at all, so it's just
    // turned off entirely via its public broadcast API rather than fought every time. In Barcode
    // mode this flips the other way — DataWedge is exactly what we want driving the trigger.
    private fun setDataWedgeScannerEnabled(context: Context, enabled: Boolean) {
        try {
            val intent = Intent("com.symbol.datawedge.api.ACTION")
            intent.putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", if (enabled) "ENABLE_PLUGIN" else "DISABLE_PLUGIN")
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // best-effort — DataWedge may not be present on non-Zebra builds of this SDK
        }
    }

    private fun configureReader(r: RFIDReader) {
        val triggerInfo = TriggerInfo()
        triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE)
        triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE)

        r.Events.addEventsListener(eventListener)
        r.Events.setHandheldEvent(true)
        r.Events.setTagReadEvent(true)
        r.Events.setAttachTagDataWithReadEvent(false)
        r.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
        r.Config.setStartTrigger(triggerInfo.StartTrigger)
        r.Config.setStopTrigger(triggerInfo.StopTrigger)

        maxPowerIndex = (r.ReaderCapabilities.getTransmitPowerLevelValues()?.size ?: 1) - 1

        val antennaConfig = r.Config.Antennas.getAntennaRfConfig(1)
        antennaConfig.setTransmitPowerIndex(maxPowerIndex)
        antennaConfig.setrfModeTableIndex(0L)
        antennaConfig.setTari(0L)
        r.Config.Antennas.setAntennaRfConfig(1, antennaConfig)

        val singulation = r.Config.Antennas.getSingulationControl(1)
        singulation.setSession(SESSION.SESSION_S0)
        singulation.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A)
        singulation.Action.setSLFlag(SL_FLAG.SL_ALL)
        r.Config.Antennas.setSingulationControl(1, singulation)

        r.Actions.PreFilters.deleteAll()
    }

    override fun isConnected(): Boolean = reader?.isConnected() == true

    // Matches Zebra's own 123RFID app and this reader's real capability table (301 entries,
    // 0-300 in 0.1 dBm steps on the tested unit) — shown and applied directly, no abstraction
    // onto a shared scale like Chainway's 1-30 dBm. coerceIn below still guards setPower()
    // against a differently-sized table on other Zebra models.
    override val powerRange: IntRange = 0..300

    override fun setPower(level: Int): Boolean {
        val r = reader ?: return false
        return try {
            val index = level.coerceIn(0, maxPowerIndex)
            val config = r.Config.Antennas.getAntennaRfConfig(1)
            config.setTransmitPowerIndex(index)
            r.Config.Antennas.setAntennaRfConfig(1, config)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun startInventory(onTag: (epc: String, rssi: String) -> Unit): String? {
        // These fallback strings stay in English regardless of app language — rare hardware
        // diagnostics, not part of the localized UI text, same as the raw vendor codes below.
        val r = reader ?: return "Reader not connected"
        if (scanning) return null
        onTagCallback = onTag
        return try {
            r.Actions.Inventory.perform()
            scanning = true
            null
        } catch (e: OperationFailureException) {
            // getMessage() doesn't carry the real reason on this SDK — the sample itself reads
            // getStatusDescription()/getVendorMessage() instead (e.g. "Charging in Progress").
            e.getStatusDescription()?.takeIf { it.isNotBlank() }
                ?: e.getVendorMessage()?.takeIf { it.isNotBlank() }
                ?: "Failed to start scan"
        } catch (e: Exception) {
            e.message ?: "Failed to start scan"
        }
    }

    override fun stopInventory() {
        scanning = false
        try {
            reader?.Actions?.Inventory?.stop()
        } catch (e: Exception) {
            // reader may already be disconnected — nothing more to do
        }
    }

    override fun setTriggerListener(onPressed: () -> Unit, onReleased: () -> Unit) {
        onTriggerPressed = onPressed
        onTriggerReleased = onReleased
    }

    // DataWedge reclaims the physical trigger button whenever this app isn't in the foreground —
    // including the moment focus is lost, not only while some other app stays in front — so
    // reasserting only on return leaves a window where a trigger press made while backgrounded
    // (background scanning enabled) fires the wrong scanner. Reassert on both transitions,
    // resending the matching DataWedge enable/disable broadcast too, since DataWedge may switch
    // to a different profile (e.g. the launcher's) on focus loss with different input enabled.
    // Both reassert whichever mode is actually current, not unconditionally RFID — reasserting
    // RFID_MODE while the operator has Barcode mode selected would silently break barcode input.
    override fun onForeground() = reassertTriggerMode()

    override fun onBackground() = reassertTriggerMode()

    override fun setInputMode(mode: InputMode) {
        currentInputMode = mode
        reassertTriggerMode()
    }

    private fun reassertTriggerMode() {
        val r = reader
        val zebraMode = if (currentInputMode == InputMode.BARCODE) ENUM_TRIGGER_MODE.BARCODE_MODE else ENUM_TRIGGER_MODE.RFID_MODE
        try {
            r?.Config?.setTriggerMode(zebraMode, true)
        } catch (e: Exception) {
            // best-effort — an explicit Start/Stop still goes through the SDK calls regardless
        }
        appContext?.let { setDataWedgeScannerEnabled(it, currentInputMode == InputMode.BARCODE) }
    }

    override fun release() {
        stopInventory()
        try {
            reader?.Events?.removeEventsListener(eventListener)
            reader?.disconnect()
        } catch (e: Exception) {
            // ignore — best-effort cleanup
        }
        try {
            Readers.deattach(this)
            readers?.Dispose()
        } catch (e: Exception) {
            // ignore
        }
        reader = null
        readers = null
    }

    override fun RFIDReaderAppeared(readerDevice: ReaderDevice?) {
        // A reader becoming available after connect() already failed isn't handled automatically
        // here — the user can just retry from the UI, matching Chainway's reconnect behavior.
    }

    override fun RFIDReaderDisappeared(readerDevice: ReaderDevice?) {
        if (readerDevice?.getName() == reader?.getHostName()) {
            scanning = false
            reader = null
        }
    }
}
