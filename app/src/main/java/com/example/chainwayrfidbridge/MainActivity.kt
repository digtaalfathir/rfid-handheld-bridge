package com.example.chainwayrfidbridge

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chainwayrfidbridge.data.AppLanguage
import com.example.chainwayrfidbridge.data.ConfigRepository
import com.example.chainwayrfidbridge.ui.DevicePickerScreen
import com.example.chainwayrfidbridge.ui.LocalStrings
import com.example.chainwayrfidbridge.ui.ScanScreen
import com.example.chainwayrfidbridge.ui.SettingsScreen
import com.example.chainwayrfidbridge.ui.stringsFor
import com.example.chainwayrfidbridge.ui.theme.ChainwayRfidTheme

private const val ROUTE_PICKER = "picker"
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
        val configRepo = ConfigRepository(this)
        val hasDeviceType = configRepo.loadDeviceType() != null
        setContent {
            ChainwayRfidTheme {
                var language by remember { mutableStateOf(configRepo.loadLanguage()) }

                CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (hasDeviceType) ROUTE_SCAN else ROUTE_PICKER
                    ) {
                        composable(
                            ROUTE_PICKER,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None }
                        ) {
                            DevicePickerScreen(onSelected = {
                                navController.navigate(ROUTE_SCAN) {
                                    popUpTo(ROUTE_PICKER) { inclusive = true }
                                }
                            })
                        }
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
                            SettingsScreen(
                                viewModel = viewModel,
                                language = language,
                                onLanguageChange = {
                                    language = it
                                    configRepo.saveLanguage(it)
                                },
                                onDone = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForeground()
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
