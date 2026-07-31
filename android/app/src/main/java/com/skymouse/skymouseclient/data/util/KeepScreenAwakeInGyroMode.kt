package com.skymouse.skymouseclient.data.util

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun KeepScreenAwakeInGyroMode(
    isGyroModeActive: Boolean,
    isGyroActive: Boolean
) {
    val currentView = LocalView.current

    DisposableEffect(isGyroModeActive, isGyroActive) {
        val window = (currentView.context as? Activity)?.window

        val shouldKeepScreenOn = isGyroModeActive && isGyroActive

        if (shouldKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
