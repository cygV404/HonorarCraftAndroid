package com.juliandobrodolac.honorarcraftandroid

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onNavigateToCreate: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val yearlyRevenueState by mainViewModel.yearlyRevenue.collectAsState()
    
    DashboardContent(
        totalSum = yearlyRevenueState,
        onNavigateToCreate = onNavigateToCreate,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        onYearChange = { year, number ->
            mainViewModel.setSelectedYear(year)
            mainViewModel.setSelectedInvoiceNumber(number.toString())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    totalSum: Double,
    onNavigateToCreate: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onYearChange: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(2026) }
    var invoiceNumberValue by remember { mutableIntStateOf(1) }

    LaunchedEffect(selectedYear, invoiceNumberValue) {
        onYearChange(selectedYear, invoiceNumberValue)
    }

    val yearlyRevenueFormatted = String.format(Locale.GERMAN, "%.2f", totalSum)

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
                    onDecrement = { if (selectedYear > 2000) selectedYear-- },
                    onIncrement = { selectedYear++ },
                    decrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    incrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                )

                DashboardControlCard(
                    title = "Rechnungsnummer",
                    value = invoiceNumberValue.toString(),
                    onDecrement = { if (invoiceNumberValue > 0) invoiceNumberValue-- },
                    onIncrement = { invoiceNumberValue++ },
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
            onNavigateToCreate = {},
            selectedTabIndex = 0,
            onTabSelected = {},
            onYearChange = { _, _ -> }
        )
    }
}
