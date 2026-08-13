package com.skymouse.skymouseclient.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.skymouse.skymouseclient.ui.connection.ConnectionViewModel
import com.skymouse.skymouseclient.ui.control.ControlViewModel
import com.skymouse.skymouseclient.ui.main.MainScreen
import com.skymouse.skymouseclient.ui.main.MainViewModel
import com.skymouse.skymouseclient.ui.settings.SettingsScreen
import com.skymouse.skymouseclient.ui.settings.SettingsViewModel

@Composable
fun Navigation(
    mainViewModel: MainViewModel,
    connectionViewModel: ConnectionViewModel,
    controlViewModel: ControlViewModel
) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        NavHost(
            navController = navController,
            startDestination = "main",

            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.88f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 6 },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 6 },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
        ) {
            composable("main") {
                AnimatedCardWrapper {
                    MainScreen(
                        mainViewModel = mainViewModel,
                        connectionViewModel = connectionViewModel,
                        controlViewModel = controlViewModel,
                        onNavigateToSettings = {
                            if (navController.currentDestination?.route != "settings") {
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
            composable("settings") {
                AnimatedCardWrapper {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedContentScope.AnimatedCardWrapper(
    content: @Composable () -> Unit
) {
    val isAnimating = transition.isRunning
    val targetRadius = if (isAnimating) 20.dp else 0.dp

    val animatedRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "cardCornerAnimation"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shape = RoundedCornerShape(animatedRadius)
                clip = true
            },
        color = MaterialTheme.colorScheme.background
    ) {
        content()
    }
}