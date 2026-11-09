package com.skymouse.skymouseclient

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.data.VolumeButtonsAction
import com.skymouse.skymouseclient.proto.KeyboardKey
import com.skymouse.skymouseclient.ui.connection.ConnectionViewModel
import com.skymouse.skymouseclient.ui.control.ControlViewModel
import com.skymouse.skymouseclient.ui.main.MainViewModel
import com.skymouse.skymouseclient.ui.navigation.Navigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val controlViewModel: ControlViewModel by viewModels()

    private val requestLocalNetwork = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {isGranted ->
      if (!isGranted) {
          Toast.makeText(this, "Permission denied, please grant access to local network", Toast.LENGTH_SHORT).show()
      }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                lifecycleScope.launch {
                    if (!connectionViewModel.isConnected.value) {
                        connectionViewModel.onConnectClicked()
                    }

                    connectionViewModel.isConnected.first {it}
                    controlViewModel.sendClipboardText(sharedText)
                }
            }
        }
    }

    private fun checkAndRequestLocalNetwork() {
        if (Build.VERSION.SDK_INT >= 37) {
            requestLocalNetwork.launch(android.Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SkyMouseManager.loadSettings(applicationContext)

        lifecycle.addObserver(mainViewModel)
        lifecycle.addObserver(controlViewModel)

        mainViewModel.onAutoConnect = { connectionViewModel.onConnectClicked() }
        mainViewModel.onDisconnect = { connectionViewModel.disconnect() }
        mainViewModel.isConnectingProvider = { connectionViewModel.isConnected.value }

        handleShareIntent(intent)

        setContent {
            val settingsState by SkyMouseManager.settingsState.collectAsStateWithLifecycle()
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            val baseColorScheme = when {
                dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
                dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            val colorScheme = if (darkTheme && settingsState.amoledDarkTheme) {
                baseColorScheme.copy(
                    background = ComposeColor.Black,
                    surface = ComposeColor.Black,
                    surfaceDim = ComposeColor.Black,
                    surfaceBright = ComposeColor(0xFF121212),
                    surfaceContainerLowest = ComposeColor.Black,
                    surfaceContainerLow = ComposeColor(0xFF080808),
                    surfaceContainer = ComposeColor(0xFF101010),
                    surfaceContainerHigh = ComposeColor(0xFF181818),
                    surfaceContainerHighest = ComposeColor(0xFF262626)
                )
            } else {
                baseColorScheme
            }

            LaunchedEffect(darkTheme, settingsState.amoledDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    )
                )
            }

            val currentHaptic = LocalHapticFeedback.current
            val customHaptic = remember(settingsState.vibrationEnabled, currentHaptic) {
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        if (settingsState.vibrationEnabled) {
                            currentHaptic.performHapticFeedback(hapticFeedbackType)
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalHapticFeedback provides customHaptic) {
                MaterialTheme(colorScheme = colorScheme){
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Navigation(
                            mainViewModel = mainViewModel,
                            connectionViewModel = connectionViewModel,
                            controlViewModel = controlViewModel
                        )
                    }
                }
            }
        }

        checkAndRequestLocalNetwork()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = SkyMouseManager.settingsState.value
        val isControlScreen = mainViewModel.currentRoute.value == "main"

        if (connectionViewModel.isConnected.value && isControlScreen && settings.volumeButtonsAction == VolumeButtonsAction.APP_CONTROL) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    controlViewModel.onKeyboardTapInput(KeyboardKey.KEY_VOL_UP, true)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    controlViewModel.onKeyboardTapInput(KeyboardKey.KEY_VOL_DOWN, true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = SkyMouseManager.settingsState.value
        val isControlScreen = mainViewModel.currentRoute.value == "main"

        if (connectionViewModel.isConnected.value && isControlScreen && settings.volumeButtonsAction == VolumeButtonsAction.APP_CONTROL) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    controlViewModel.onKeyboardTapInput(KeyboardKey.KEY_VOL_UP, false)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    controlViewModel.onKeyboardTapInput(KeyboardKey.KEY_VOL_DOWN, false)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}
