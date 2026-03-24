package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aandios.nous.feature.dom.domain.DepthLimit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthLimitSelector(
    currentLimit: DepthLimit,
    onLimitChanged: (DepthLimit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val standardValues = DepthLimit.standardValues

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${currentLimit.value} levels",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            label = { Text("Depth Limit") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            standardValues.forEach { value ->
                DropdownMenuItem(
                    text = { Text("$value levels") },
                    onClick = {
                        onLimitChanged(DepthLimit.create(value))
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
            
            // Кастомное значение
            DropdownMenuItem(
                text = { Text("Custom...") },
                onClick = {
                    // TODO: Реализовать диалог для ввода кастомного значения
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }
}