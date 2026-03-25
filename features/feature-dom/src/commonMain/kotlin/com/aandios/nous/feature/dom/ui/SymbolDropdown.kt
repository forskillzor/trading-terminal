package com.aandios.nous.feature.dom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aandios.nous.feature.dom.domain.TradingProvider
import com.aandios.nous.feature.dom.domain.TradingSymbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDropdown(
    currentSymbol: TradingSymbol,
    provider: TradingProvider,
    onSymbolChanged: (TradingSymbol) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val symbols = remember(provider) {
        TradingSymbol.getSymbolsForProvider(provider)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currentSymbol.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            label = { Text("Symbol") },
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)

        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            symbols.forEach { symbol ->
                DropdownMenuItem(
                    text = { Text(symbol.displayName, fontSize = 12.sp) },
                    onClick = {
                        onSymbolChanged(symbol)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}