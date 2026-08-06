package com.example.chainwayrfidbridge

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.chainwayrfidbridge.data.ConfigRepository
import com.example.chainwayrfidbridge.service.FloatingScanService

/**
 * Watches whole-app foreground/background transitions (not individual Activity callbacks, which
 * also fire on things like rotation) to show/hide the floating scan bubble. The actual scan
 * session itself lives in ScanViewModel and keeps running regardless — this only toggles the
 * bubble's visibility to match whether MainActivity is actually on screen.
 */
class RfidBridgeApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (ConfigRepository(this).load().backgroundScanEnabled) {
            FloatingScanService.start(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        FloatingScanService.stop(this)
    }
}
