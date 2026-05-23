package com.usbdroid

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.usbdroid.core.detection.USBConnectionEvent
import com.usbdroid.ui.screens.*
import com.usbdroid.ui.theme.USBDroidTheme
import com.usbdroid.viewmodel.MainViewModel
import com.usbdroid.service.USBBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle USB intent
        handleUsbIntent(intent)

        setContent {
            USBDroidTheme {
                val navController = rememberNavController()
                USBDroidApp(navController, viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        intent?.let {
            if (it.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val device = it.getParcelableExtra<android.hardware.usb.UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let { usbDevice ->
                    Timber.i("USB device attached via intent: ${usbDevice.deviceName}")
                    viewModel.onDeviceAttached(usbDevice)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}

@Composable
fun USBDroidApp(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val detectedDevices by viewModel.detectedDevices.collectAsStateWithLifecycle()
    val connectionEvents by viewModel.connectionEvents.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != Screen.Splash.route) {
                BottomNavBar(navController, currentRoute)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onDeviceClick = { deviceInfo ->
                        val moduleId = viewModel.getSuggestedModule(deviceInfo)
                        navController.navigate(Screen.Module.createRoute(moduleId))
                    },
                    onOpenAnalyzer = {
                        navController.navigate(Screen.Module.createRoute("analyzer"))
                    }
                )
            }
            composable(Screen.Modules.route) {
                ModulesScreen(viewModel = viewModel)
            }
            composable(Screen.Logs.route) {
                LogsScreen(viewModel = viewModel)
            }
            composable(Screen.AI.route) {
                AIScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(Screen.Module.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: "analyzer"
                ModuleDetailScreen(
                    moduleId = moduleId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Modules : Screen("modules")
    object Logs : Screen("logs")
    object AI : Screen("ai")
    object Settings : Screen("settings")
    object Module : Screen("module/{moduleId}") {
        fun createRoute(moduleId: String) = "module/$moduleId"
    }
}
