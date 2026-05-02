package com.aandios.nous_platform.ui.dom

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aandios.nous.feature.dom.ui.DomViewModel
import com.aandios.nous_platform.ui.theme.TradingTerminalTheme
import org.koin.compose.koinInject

@Composable
private fun DomPreview(
    domViewModel: DomViewModel
) {
    DomWidget(
        domViewModel = domViewModel,
        modifier = Modifier.width(300.dp)
    )
}

fun main() = application {
//    initKoin()
    val domViewModel: DomViewModel = koinInject()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous Platform • v 0.1",
        state = rememberWindowState(width = 300.dp, height = 1200.dp)
    ) {
        TradingTerminalTheme(
            darkTheme = true,
            nightMode = false
        ) {
            DomPreview(domViewModel)
        }
    }
}
