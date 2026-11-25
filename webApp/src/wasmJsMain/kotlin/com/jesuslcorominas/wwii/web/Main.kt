package com.jesuslcorominas.wwii.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jesuslcorominas.wwii.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { App() }
}
