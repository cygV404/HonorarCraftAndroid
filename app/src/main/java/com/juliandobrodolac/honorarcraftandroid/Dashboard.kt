package com.juliandobrodolac.honorarcraftandroid

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juliandobrodolac.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import java.util.Locale

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel,

) {
    val yearlyRevenueState by mainViewModel.yearlyRevenue.collectAsState()
    val selectedYear by mainViewModel.selectedYear.collectAsState()
    val selectedInvoiceNumber by mainViewModel.selectedInvoiceNumber.collectAsState()
    
    DashboardContent(
        totalSum = yearlyRevenueState,
        selectedYear = selectedYear,
        invoiceNumber = selectedInvoiceNumber,
        onYearChange = { mainViewModel.setSelectedYear(it) }
    ) { mainViewModel.setSelectedInvoiceNumber(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    totalSum: Double,
    selectedYear: Int,
    invoiceNumber: String,
    onYearChange: (Int) -> Unit,
    onInvoiceNumberChange: (String) -> Unit
) {
    val yearlyRevenueFormatted = String.format(Locale.GERMAN, "%.2f", totalSum)
    val invoiceNumberInt = invoiceNumber.toIntOrNull() ?: 1

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "HonorarCraft",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.primary,
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DashboardControlCard(
                    title = "Jahresumsatz $selectedYear",
                    value = "$yearlyRevenueFormatted €",
                    onDecrement = { if (selectedYear > 2000) onYearChange(selectedYear - 1) },
                    onIncrement = { onYearChange(selectedYear + 1) },
                    decrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    incrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                )

                DashboardControlCard(
                    title = "Rechnungsnummer",
                    value = invoiceNumber,
                    onDecrement = { if (invoiceNumberInt > 0) onInvoiceNumberChange((invoiceNumberInt - 1).toString()) },
                    onIncrement = { onInvoiceNumberChange((invoiceNumberInt + 1).toString()) },
                    decrementIcon = Icons.Default.Remove,
                    incrementIcon = Icons.Default.Add
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DashboardControlCard(
    title: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementIcon: ImageVector,
    incrementIcon: ImageVector
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDecrement) {
                Icon(decrementIcon, contentDescription = "Verringern", tint = MaterialTheme.colorScheme.primary)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "valueAnimation"
                ) { targetValue ->
                    Text(text = targetValue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onIncrement) {
                Icon(incrementIcon, contentDescription = "Erhöhen", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun DashboardPreview() {
    HonorarCraftAndroidTheme {
        DashboardContent(
            totalSum = 12500.0,
            selectedYear = 2026,
            invoiceNumber = "1",
            onYearChange = { }
        ) { }
    }
}
