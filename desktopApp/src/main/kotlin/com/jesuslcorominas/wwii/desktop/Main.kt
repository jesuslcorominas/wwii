package com.jesuslcorominas.wwii.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jesuslcorominas.wwii.ui.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "WWII") {
            App()
        }
    }
