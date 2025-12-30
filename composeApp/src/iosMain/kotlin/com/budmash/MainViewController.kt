package com.budmash

import androidx.compose.ui.window.ComposeUIViewController
import com.budmash.capture.initializeIosImageCapture
import com.budmash.ui.App

fun MainViewController() = ComposeUIViewController {
    // Initialize iOS-specific services
    initializeIosImageCapture()

    App()
}
