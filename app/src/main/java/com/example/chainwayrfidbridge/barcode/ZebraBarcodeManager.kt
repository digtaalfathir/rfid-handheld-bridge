package com.example.chainwayrfidbridge.barcode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle

private const val DATAWEDGE_ACTION = "com.symbol.datawedge.api.ACTION"
private const val PROFILE_NAME = "StechoqRFIDSuiteBarcode"

/**
 * Zebra barcode backend: DataWedge itself IS the barcode engine here, there's no separate SDK to
 * call the way Chainway has one. Once RfidReaderManager.setBarcodeTriggerActive(true) hands the
 * physical trigger to DataWedge (ENUM_TRIGGER_MODE.BARCODE_MODE), DataWedge owns the whole
 * press-scan-decode sequence itself — this class only configures a dedicated DataWedge profile to
 * deliver results to our own broadcast action, and listens for them. startScan()/stopScan() are
 * genuinely no-ops here; nothing on our side drives the actual scan.
 *
 * Unlike the RFID side (verified against real SDK bytecode), this profile/intent-output
 * configuration is built from Zebra's publicly documented DataWedge API. First live-device pass
 * showed the trigger correctly reaching the scan engine (decode succeeds) but the result going
 * nowhere — the custom profile's APP_LIST association wasn't reliably becoming the *active*
 * profile, so DataWedge fell back to some other profile's default keystroke output, which a
 * Compose UI with nothing focused just swallows. configureProfile() now also explicitly
 * SWITCH_TO_PROFILEs after configuring, instead of trusting automatic foreground-app detection.
 * If results still don't arrive, check the DataWedge app itself to confirm
 * "StechoqRFIDSuiteBarcode" exists, is enabled, and PARAM_LIST key names match the DataWedge
 * version on the handheld.
 */
class ZebraBarcodeManager : BarcodeReaderManager {

    private var appContext: Context? = null
    private var onResult: ((String) -> Unit)? = null
    private var registered = false

    private fun resultAction(packageName: String) = "$packageName.BARCODE_DATA"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val data = intent.getStringExtra("com.symbol.datawedge.data_string")
            if (!data.isNullOrEmpty()) onResult?.invoke(data)
        }
    }

    override fun connect(context: Context): Boolean {
        val ctx = context.applicationContext
        appContext = ctx
        return try {
            configureProfile(ctx)
            if (!registered) {
                @Suppress("UnspecifiedRegisterReceiverFlag") // targetSdk 32 — legacy registration still valid
                ctx.registerReceiver(receiver, IntentFilter(resultAction(ctx.packageName)))
                registered = true
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setResultListener(onResult: (String) -> Unit) {
        this.onResult = onResult
    }

    override fun release() {
        val ctx = appContext
        if (registered && ctx != null) {
            try {
                ctx.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore — already unregistered
            }
        }
        registered = false
        onResult = null
    }

    /** Creates (or updates) a DataWedge profile scoped to this app, with barcode results routed
     * to our own broadcast action instead of DataWedge's default keystroke output — so we get a
     * structured result callback instead of needing a focused EditText to "type" into.
     *
     * Split into the same one-thing-per-broadcast steps as Zebra's own DataWedge API guide
     * samples, rather than folding APP_LIST into the CREATE_IF_NOT_EXIST call — some DataWedge
     * versions only pick up the app association reliably when it arrives as its own UPDATE.
     * Ends with an explicit SWITCH_TO_PROFILE: relying on automatic foreground-app association
     * left results going to whatever profile actually was active (default keystroke output, which
     * silently swallows the scan since nothing has focus) — forcing the switch removes that
     * association-timing gap entirely. */
    private fun configureProfile(context: Context) {
        val packageName = context.packageName

        sendSetConfig(context, Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST")
        })

        val appConfig = Bundle().apply {
            putString("PACKAGE_NAME", packageName)
            putStringArray("ACTIVITY_LIST", arrayOf("*"))
        }
        sendSetConfig(context, Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")
            putParcelableArrayList("APP_LIST", arrayListOf(appConfig))
        })

        // Make sure the barcode input plugin itself is on — belt-and-braces alongside the global
        // SCANNER_INPUT_PLUGIN enable ZebraReaderManager sends when entering Barcode mode.
        val barcodeParams = Bundle().apply {
            putString("scanner_input_enabled", "true")
            putString("scanner_selection", "auto")
        }
        sendSetConfig(context, Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")
            putBundle("PLUGIN_CONFIG", Bundle().apply {
                putString("PLUGIN_NAME", "BARCODE")
                putString("RESET_CONFIG", "true")
                putBundle("PARAM_LIST", barcodeParams)
            })
        })

        // Disable keystroke output explicitly so a decode can never silently vanish into an
        // unfocused view — intent output below is the only path results should take.
        sendSetConfig(context, Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")
            putBundle("PLUGIN_CONFIG", Bundle().apply {
                putString("PLUGIN_NAME", "KEYSTROKE")
                putString("RESET_CONFIG", "true")
                putBundle("PARAM_LIST", Bundle().apply {
                    putString("keystroke_output_enabled", "false")
                })
            })
        })

        val intentParams = Bundle().apply {
            putString("intent_output_enabled", "true")
            putString("intent_action", resultAction(packageName))
            putString("intent_delivery", "2") // 2 = broadcast intent
        }
        sendSetConfig(context, Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")
            putBundle("PLUGIN_CONFIG", Bundle().apply {
                putString("PLUGIN_NAME", "INTENT")
                putString("RESET_CONFIG", "true")
                putBundle("PARAM_LIST", intentParams)
            })
        })

        val intent = Intent(DATAWEDGE_ACTION)
        intent.putExtra("com.symbol.datawedge.api.SWITCH_TO_PROFILE", PROFILE_NAME)
        context.sendBroadcast(intent)
    }

    private fun sendSetConfig(context: Context, config: Bundle) {
        val intent = Intent(DATAWEDGE_ACTION)
        intent.putExtra("com.symbol.datawedge.api.SET_CONFIG", config)
        context.sendBroadcast(intent)
    }
}
