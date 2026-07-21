package com.example.chainwayrfidbridge

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chainwayrfidbridge.ui.ScanScreen
import com.example.chainwayrfidbridge.ui.SettingsScreen
import com.example.chainwayrfidbridge.ui.theme.ChainwayRfidTheme

private const val ROUTE_SCAN = "scan"
private const val ROUTE_SETTINGS = "settings"

/**
 * Raw evdev scancode of this device's physical scan trigger (input device "scan-key",
 * /dev/input/event0). It maps to a non-standard Android keycode (HANDLE_KEY, not a real
 * KeyEvent.KEYCODE_*), so we match on scanCode rather than keyCode.
 */
private const val SCAN_TRIGGER_SCANCODE = 185

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChainwayRfidTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = ROUTE_SCAN) {
                    composable(
                        ROUTE_SCAN,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None }
                    ) {
                        ScanScreen(viewModel = viewModel, onOpenSettings = { navController.navigate(ROUTE_SETTINGS) })
                    }
                    composable(
                        ROUTE_SETTINGS,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None }
                    ) {
                        SettingsScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.scanCode == SCAN_TRIGGER_SCANCODE) {
            if (event.repeatCount == 0) viewModel.onTriggerPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.scanCode == SCAN_TRIGGER_SCANCODE) {
            viewModel.onTriggerReleased()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
