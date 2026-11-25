package com.jesuslcorominas.wwii.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jesuslcorominas.wwii.core.GameInfo

/** Root composable shared by every launcher. */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = GameInfo.summary(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
