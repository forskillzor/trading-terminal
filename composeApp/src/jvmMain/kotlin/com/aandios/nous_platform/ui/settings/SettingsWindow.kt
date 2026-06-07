package com.aandios.nous_platform.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.aandios.nous_platform.storage.LocalStorage
import kotlinx.coroutines.launch

@Composable
fun SettingsWindow(
    storage: LocalStorage,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Storage")

    DialogWindow(
        onCloseRequest = onClose,
        title = "Settings",
        state = rememberDialogState(width = 520.dp, height = 420.dp)
    ) {
        Surface(
            color = Color(0xFF0A0E14),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF12171F)) {
                    tabs.forEachIndexed { i, name ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(name, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> StorageTab(storage)
                }
            }
        }
    }
}

@Composable
private fun StorageTab(storage: LocalStorage) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var confirmAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        stats = storage.getStats()
    }

    fun refresh() { scope.launch { stats = storage.getStats() } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Local Storage", color = Color(0xFF5B9BD5), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        // Stats
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12171F))) {
            Column(Modifier.padding(12.dp)) {
                StatsRow("Settings keys", stats["settings"] ?: 0)
                StatsRow("Candles cached", stats["candles"] ?: 0)
                StatsRow("Footprint cached", stats["footprint"] ?: 0)
                StatsRow("DB file size", stats["db_size"] ?: 0, format = ::formatBytes)
            }
        }

        // Clear buttons
        Text("Clear data", color = Color(0xFF888888), fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        ClearButton("Clear Settings", "settings") { confirmAction = it }
        ClearButton("Clear Candles Cache", "candles") { confirmAction = it }
        ClearButton("Clear Footprint Cache", "footprint") { confirmAction = it }
        ClearButton("Clear ALL Data", "all") { confirmAction = it }

        // Confirmation dialog
        if (confirmAction != null) {
            AlertDialog(
                onDismissRequest = { confirmAction = null },
                title = { Text("Confirm deletion", color = Color(0xFFF07178)) },
                text = { Text("Are you sure you want to clear ${confirmAction}? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                when (confirmAction) {
                                    "settings" -> storage.clearSettings()
                                    "candles" -> storage.clearCandles()
                                    "footprint" -> storage.clearFootprint()
                                    "all" -> storage.clearAll()
                                }
                                confirmAction = null
                                refresh()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmAction = null }) {
                        Text("Cancel", color = Color(0xFF888888))
                    }
                }
            )
        }
    }
}

@Composable
private fun StatsRow(label: String, value: Long, format: (Long) -> String = { it.toString() }) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF888888), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text(format(value), color = Color(0xFF5B9BD5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun ClearButton(label: String, action: String, onConfirm: (String) -> Unit) {
    Button(
        onClick = { onConfirm(action) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(label, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "${"%.1f".format(bytes / 1_000_000.0)} MB"
    bytes >= 1_000 -> "${"%.1f".format(bytes / 1_000.0)} KB"
    else -> "$bytes B"
}
