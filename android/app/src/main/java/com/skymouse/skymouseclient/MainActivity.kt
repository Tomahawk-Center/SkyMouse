package com.skymouse.skymouseclient

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.ui.connection.ConnectionViewModel
import com.skymouse.skymouseclient.ui.control.ControlViewModel
import com.skymouse.skymouseclient.ui.main.MainViewModel
import com.skymouse.skymouseclient.ui.navigation.Navigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            val colorScheme = when {
                dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
                dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    )
                )
            }

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

        checkAndRequestLocalNetwork()
    }
}