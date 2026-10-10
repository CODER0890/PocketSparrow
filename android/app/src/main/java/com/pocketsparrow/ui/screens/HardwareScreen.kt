package com.pocketsparrow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketsparrow.ui.components.HardwareAcceleratorDashboard
import com.pocketsparrow.ui.theme.LocalSparrowColors

@Composable
fun HardwareScreen() {
    val colors = LocalSparrowColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            HardwareAcceleratorDashboard()
        }
    }
}
