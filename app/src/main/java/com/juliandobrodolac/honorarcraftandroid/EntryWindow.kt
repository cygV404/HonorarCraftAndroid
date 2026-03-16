package com.juliandobrodolac.honorarcraftandroid

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.juliandobrodolac.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun EntryWindowScreen(
    mainViewModel: MainViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val currentInvoiceWithEntries by mainViewModel.currentInvoiceWithEntries.collectAsState()
    val formattedInvoiceNumber by mainViewModel.formattedInvoiceNumber.collectAsState()
    val allInvoiceNumbers by mainViewModel.allInvoiceNumbers.collectAsState()
    val invoiceFormat by mainViewModel.invoiceFormat.collectAsState()
    val companyData by mainViewModel.companyData.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()

    EntryWindowContent(
        displayInvoiceNumber = formattedInvoiceNumber,
        allInvoiceNumbers = allInvoiceNumbers,
        invoiceFormat = invoiceFormat,
        invoiceWithEntries = currentInvoiceWithEntries,
        companyData = companyData,
        isLoading = isLoading,
        onInvoiceSelect = { mainViewModel.setSelectedInvoiceNumber(it) },
        onGeneratePdf = { context, cd, iwe ->
            mainViewModel.viewModelScope.launch {
                mainViewModel.setLoading(true)
                delay(1500) // Simulation for wavy effect visibility
                createPdf(context, cd, iwe)
                mainViewModel.incrementInvoiceNumber()
                mainViewModel.setLoading(false)
            }
        },
        onDeleteEntries = { mainViewModel.deleteEntries(it) },
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryWindowContent(
    displayInvoiceNumber: String,
    allInvoiceNumbers: List<String>,
    invoiceFormat: InvoiceFormat,
    invoiceWithEntries: InvoiceWithEntries?,
    companyData: CompanyData?,
    isLoading: Boolean,
    onInvoiceSelect: (String) -> Unit,
    onGeneratePdf: (android.content.Context, CompanyData, InvoiceWithEntries) -> Unit,
    onDeleteEntries: (List<InvoiceEntry>) -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isDescending by remember { mutableStateOf(true) }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            TopAppBar(
                title = { Text("${selectedIds.size} ausgewählt") },
                navigationIcon = {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Auswahl aufheben")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Ausgewählte löschen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        } else {
            TopAppBar(
                title = {
                    Text(
                        "Rechnung $displayInvoiceNumber",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.FormatListNumbered,
                                contentDescription = "Optionen",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            allInvoiceNumbers.forEach { num ->
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH) + 1
                                val formattedNum = when (invoiceFormat) {
                                    InvoiceFormat.NUMBER -> num
                                    InvoiceFormat.YEAR_NUMBER -> "$year-$num"
                                    InvoiceFormat.YEAR_MONTH_NUMBER -> String.format(
                                        Locale.GERMANY,
                                        "%d-%02d-%s",
                                        year,
                                        month,
                                        num
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Rechnung $formattedNum") },
                                    onClick = {
                                        showMenu = false
                                        onInvoiceSelect(num)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isDescending) "Sortierung: Absteigend" else "Sortierung: Aufsteigend") },
                                leadingIcon = {
                                    Icon(
                                        if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isDescending = !isDescending
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                val totalUe =
                    String.format(Locale.GERMAN, "%.2f", invoiceWithEntries?.totalLessonUnit ?: 0.0)
                val totalSum =
                    String.format(Locale.GERMAN, "%.2f €", invoiceWithEntries?.totalSum ?: 0.0)

                SummaryCard(totalUe = totalUe, totalSum = totalSum)

                Spacer(modifier = Modifier.height(16.dp))

                // Sorting logic: Respects the isDescending state
                val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }
                val sortedEntries = remember(invoiceWithEntries, isDescending) {
                    val entries = invoiceWithEntries?.entries ?: emptyList()
                    if (isDescending) {
                        entries.sortedWith(
                            compareByDescending<InvoiceEntry> {
                                try {
                                    dateFormat.parse(it.date)
                                } catch (e: Exception) {
                                    null
                                }
                            }.thenByDescending { it.id }
                        )
                    } else {
                        entries.sortedWith(
                            compareBy<InvoiceEntry> {
                                try {
                                    dateFormat.parse(it.date)
                                } catch (e: Exception) {
                                    null
                                }
                            }.thenBy { it.id }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = sortedEntries,
                        key = { it.id }
                    ) { entry ->
                        val rate = invoiceWithEntries?.invoice?.rate ?: 0.0
                        val ueValue = (entry.lessonUnits * 60) / 45
                        val entrySum = ueValue * rate

                        EntryCard(
                            entry = InvoiceEntryData(
                                date = entry.date,
                                ue = String.format(Locale.GERMAN, "%.2f UE", ueValue),
                                subject = entry.teachingSubject,
                                total = String.format(Locale.GERMAN, "%.2f €", entrySum)
                            ),
                            isSelected = selectedIds.contains(entry.id),
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (selectedIds.contains(entry.id)) {
                                        selectedIds - entry.id
                                    } else {
                                        selectedIds + entry.id
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedIds = setOf(entry.id)
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (companyData != null && invoiceWithEntries != null) {
                            onGeneratePdf(context, companyData, invoiceWithEntries)
                        } else {
                            Toast.makeText(context, "Daten unvollständig", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF generieren")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    // Multiple Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Einträge löschen?") },
            text = { Text("Möchtet du die ${selectedIds.size} ausgewählten Einträge wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val entriesToDelete =
                            invoiceWithEntries?.entries?.filter { it.id in selectedIds }
                                ?: emptyList()
                        onDeleteEntries(entriesToDelete)
                        selectedIds = emptySet()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

data class InvoiceEntryData(
    val date: String,
    val ue: String,
    val subject: String,
    val total: String
)

@Composable
fun SummaryCard(totalUe: String, totalSum: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Summe gesamt: $totalSum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "UE gesamt: $totalUe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    entry: InvoiceEntryData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${entry.ue} - ${entry.subject}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = entry.total,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun EntryWindowPreview() {
    HonorarCraftAndroidTheme {
        EntryWindowContent(
            displayInvoiceNumber = "1",
            allInvoiceNumbers = listOf("1", "2"),
            invoiceFormat = InvoiceFormat.NUMBER,
            invoiceWithEntries = null,
            companyData = null,
            isLoading = false,
            onInvoiceSelect = {},
            onGeneratePdf = { _, _, _ -> },
            onDeleteEntries = {},
            selectedTabIndex = 2,
            onTabSelected = {}
        )
    }
}
