package de.v404.honorarcraftandroid

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.v404.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel,
) {
    val yearlyRevenueState by mainViewModel.yearlyRevenue.collectAsState()
    val dashboardYear by mainViewModel.dashboardYear.collectAsState()
    val invoiceYear by mainViewModel.invoiceYear.collectAsState()
    val invoiceMonth by mainViewModel.invoiceMonth.collectAsState()
    val rawInvoiceNumber by mainViewModel.selectedInvoiceNumber.collectAsState()
    val formattedInvoiceNumber by mainViewModel.formattedInvoiceNumber.collectAsState()
    val invoiceFormat by mainViewModel.invoiceFormat.collectAsState()

    DashboardContent(
        totalSum = yearlyRevenueState,
        dashboardYear = dashboardYear,
        invoiceYear = invoiceYear,
        invoiceMonth = invoiceMonth,
        rawInvoiceNumber = rawInvoiceNumber,
        displayInvoiceNumber = formattedInvoiceNumber,
        invoiceFormat = invoiceFormat,
        onDashboardYearChange = { mainViewModel.setDashboardYear(it) },
        onInvoiceYearChange = { mainViewModel.setInvoiceYear(it) },
        onInvoiceMonthChange = { mainViewModel.setInvoiceMonth(it) },
        onInvoiceNumberChange = { mainViewModel.setSelectedInvoiceNumber(it) },
        onFormatChange = { mainViewModel.setInvoiceFormat(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    totalSum: BigDecimal,
    dashboardYear: Int,
    invoiceYear: Int,
    invoiceMonth: Int,
    rawInvoiceNumber: String,
    displayInvoiceNumber: String,
    invoiceFormat: InvoiceFormat,
    onDashboardYearChange: (Int) -> Unit,
    onInvoiceYearChange: (Int) -> Unit,
    onInvoiceMonthChange: (Int) -> Unit,
    onInvoiceNumberChange: (String) -> Unit,
    onFormatChange: (InvoiceFormat) -> Unit
) {
    val decimalFormat = remember { DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY)) }
    val yearlyRevenueFormatted = decimalFormat.format(totalSum)
    val invoiceNumberInt = rawInvoiceNumber.toIntOrNull() ?: 1
    var showMenu by remember { mutableStateOf(false) }

    var showEditInvoiceDialog by remember { mutableStateOf(false) }

    if (showEditInvoiceDialog) {
        var editYear by remember { mutableStateOf(invoiceYear.toString()) }
        var editMonth by remember { mutableStateOf(String.format(Locale.GERMANY, "%02d", invoiceMonth)) }
        var editNumber by remember { mutableStateOf(rawInvoiceNumber) }

        AlertDialog(
            onDismissRequest = { showEditInvoiceDialog = false },
            title = { Text("Rechnungsnummer bearbeiten") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (invoiceFormat == InvoiceFormat.YEAR_NUMBER || invoiceFormat == InvoiceFormat.YEAR_MONTH_NUMBER) {
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) editYear = it },
                            modifier = Modifier.width(85.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                        )
                        Text(" - ", style = MaterialTheme.typography.headlineSmall)
                    }

                    if (invoiceFormat == InvoiceFormat.YEAR_MONTH_NUMBER) {
                        OutlinedTextField(
                            value = editMonth,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) editMonth = it },
                            modifier = Modifier.width(60.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                        )
                        Text(" - ", style = MaterialTheme.typography.headlineSmall)
                    }

                    OutlinedTextField(
                        value = editNumber,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editNumber = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val y = editYear.toIntOrNull() ?: invoiceYear
                    val m = editMonth.toIntOrNull() ?: invoiceMonth
                    if (editNumber.isNotEmpty()) {
                        onInvoiceYearChange(y)
                        onInvoiceMonthChange(m)
                        onInvoiceNumberChange(editNumber)
                    }
                    showEditInvoiceDialog = false
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInvoiceDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "HonorarCraft",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Format: Nr.") },
                            onClick = {
                                onFormatChange(InvoiceFormat.NUMBER)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Format: Jahr-Nr.") },
                            onClick = {
                                onFormatChange(InvoiceFormat.YEAR_NUMBER)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Format: Jahr-Monat-Nr.") },
                            onClick = {
                                onFormatChange(InvoiceFormat.YEAR_MONTH_NUMBER)
                                showMenu = false
                            }
                        )
                    }
                }
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
                    title = "Jahresumsatz $dashboardYear",
                    value = "$yearlyRevenueFormatted €",
                    onDecrement = { if (dashboardYear > 2000) onDashboardYearChange(dashboardYear - 1) },
                    onIncrement = { onDashboardYearChange(dashboardYear + 1) },
                    decrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    incrementIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                )

                DashboardControlCard(
                    title = "Rechnungsnummer",
                    value = displayInvoiceNumber,
                    onDecrement = {
                        if (invoiceNumberInt > 0) {
                            val next = invoiceNumberInt - 1
                            val formatted = String.format(Locale.GERMANY, "%0${rawInvoiceNumber.length}d", next)
                            onInvoiceNumberChange(formatted)
                        }
                    },
                    onIncrement = {
                        val next = invoiceNumberInt + 1
                        val formatted = String.format(Locale.GERMANY, "%0${rawInvoiceNumber.length}d", next)
                        onInvoiceNumberChange(formatted)
                    },
                    decrementIcon = Icons.Default.Remove,
                    incrementIcon = Icons.Default.Add,
                    onClick = { showEditInvoiceDialog = true }
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
    incrementIcon: ImageVector,
    onClick: (() -> Unit)? = null
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
                Icon(
                    decrementIcon,
                    contentDescription = "Verringern",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "valueAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onIncrement) {
                Icon(
                    incrementIcon,
                    contentDescription = "Erhöhen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun DashboardPreview() {
    HonorarCraftAndroidTheme {
        DashboardContent(
            totalSum = BigDecimal("12500.00"),
            dashboardYear = 2026,
            invoiceYear = 2026,
            invoiceMonth = 3,
            rawInvoiceNumber = "1",
            displayInvoiceNumber = "1",
            invoiceFormat = InvoiceFormat.NUMBER,
            onDashboardYearChange = { },
            onInvoiceYearChange = { },
            onInvoiceMonthChange = { },
            onInvoiceNumberChange = { },
            onFormatChange = { }
        )
    }
}
