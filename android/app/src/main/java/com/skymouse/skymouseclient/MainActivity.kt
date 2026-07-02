package com.skymouse.skymouseclient

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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.skymouse.skymouseclient.ui.MainScreen
import com.skymouse.skymouseclient.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private val requestLocalNetwork = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {isGranted ->
      if (!isGranted) {
          Toast.makeText(this, "Permission denied, please grant access to local network", Toast.LENGTH_SHORT).show()
      }
    }

    private fun checkAndRequestLocalNetwork() {
        if (Build.VERSION.SDK_INT >= 37) {
            requestLocalNetwork.launch(android.Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()

            val colorScheme = if (darkTheme){
                darkColorScheme()
            } else {
                lightColorScheme()
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
                    MainScreen(viewModel = mainViewModel)
                }
            }
        }

        checkAndRequestLocalNetwork()
    }
}