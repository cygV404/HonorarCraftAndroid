package de.v404.honorarcraftandroid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.v404.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CreateInvoiceScreen(
    mainViewModel: MainViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val formattedInvoiceNumber by mainViewModel.formattedInvoiceNumber.collectAsState()
    val allInvoiceNumbers by mainViewModel.allInvoiceNumbers.collectAsState()
    val invoiceFormat by mainViewModel.invoiceFormat.collectAsState()

    val datum by mainViewModel.currentDate.collectAsState()
    val stunden by mainViewModel.currentHours.collectAsState()
    val klasseFach by mainViewModel.currentSubject.collectAsState()
    val uniqueSubjects by mainViewModel.uniqueSubjects.collectAsState()

    val lastAddedEntry by mainViewModel.lastAddedEntry.collectAsState()
    val showConfirmation by mainViewModel.showEntryConfirmation.collectAsState()

    CreateInvoiceContent(
        displayInvoiceNumber = formattedInvoiceNumber,
        allInvoiceNumbers = allInvoiceNumbers,
        invoiceFormat = invoiceFormat,
        datum = datum,
        stunden = stunden,
        klasseFach = klasseFach,
        uniqueSubjects = uniqueSubjects,
        onUpdateForm = { d, s, kf -> mainViewModel.updateInvoiceForm(d, s, kf) },
        onAddEntry = { mainViewModel.addEntryFromForm() },
        onInvoiceSelect = { mainViewModel.setSelectedInvoiceNumber(it) },
        onDeleteSubjectSuggestion = { mainViewModel.deleteSubjectSuggestion(it) },
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        lastAddedEntry = lastAddedEntry,
        showConfirmation = showConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceContent(
    displayInvoiceNumber: String,
    allInvoiceNumbers: List<String>,
    invoiceFormat: InvoiceFormat,
    datum: String,
    stunden: String,
    klasseFach: String,
    uniqueSubjects: List<String>,
    onUpdateForm: (String?, String?, String?) -> Unit,
    onAddEntry: () -> Unit,
    onInvoiceSelect: (String) -> Unit,
    onDeleteSubjectSuggestion: (String) -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    lastAddedEntry: InvoiceEntry? = null,
    showConfirmation: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val focusRequesterStunden = remember { FocusRequester() }
    val focusRequesterSubject = remember { FocusRequester() }

    var expandedSubject by remember { mutableStateOf(false) }

    val canAddEntry = datum.isNotBlank() && stunden.isNotBlank()

    // Filter logic: Show all if empty, otherwise filter by start. Sorted by frequency (handled by DAO).
    val filteredSubjects = if (klasseFach.isBlank()) {
        uniqueSubjects
    } else {
        uniqueSubjects.filter {
            it.startsWith(klasseFach, ignoreCase = true) && it.equals(klasseFach, ignoreCase = true)
                .not()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.primary,
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            // Main content area with background and centered form
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

                Column(
                    modifier = Modifier
                        .width(380.dp)
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = datum,
                            onValueChange = { },
                            label = { Text("Datum") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Datum wählen"
                                    )
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = stunden,
                        onValueChange = { onUpdateForm(null, it, null) },
                        label = { Text("Stunden") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterStunden),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusRequesterSubject.requestFocus() }
                        )
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedSubject && filteredSubjects.isNotEmpty(),
                        onExpandedChange = { expandedSubject = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = klasseFach,
                            onValueChange = {
                                onUpdateForm(null, null, it)
                                expandedSubject = true
                            },
                            label = { Text("Klasse/Fach") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .focusRequester(focusRequesterSubject)
                                .onFocusChanged { if (it.isFocused) expandedSubject = true },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    expandedSubject = false
                                    if (canAddEntry) {
                                        onAddEntry()
                                    }
                                }
                            )
                        )

                        if (filteredSubjects.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedSubject,
                                onDismissRequest = { expandedSubject = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                filteredSubjects.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectionOption,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                IconButton(
                                                    onClick = {
                                                        onDeleteSubjectSuggestion(
                                                            selectionOption
                                                        )
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Löschen",
                                                        tint = MaterialTheme.colorScheme.error.copy(
                                                            alpha = 0.6f
                                                        ),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onUpdateForm(null, null, selectionOption)
                                            expandedSubject = false
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Confirmation Card overlaying the top (using the top-level AnimatedVisibility)
            androidx.compose.animation.AnimatedVisibility(
                visible = showConfirmation && lastAddedEntry != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .width(380.dp)
                    .padding(horizontal = 16.dp)
            ) {
                lastAddedEntry?.let { entry ->
                    val rate = entry.rate
                    val ueValue = entry.lessonUnits.multiply(BigDecimal("60"))
                        .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
                    val entrySum = ueValue.multiply(rate).setScale(2, RoundingMode.HALF_UP)

                    val formattedUe = String.format(Locale.GERMAN, "%,.2f UE", ueValue)
                    val formattedTotal = String.format(Locale.GERMAN, "%,.2f €", entrySum)

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$formattedUe - ${entry.teachingSubject}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formattedTotal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (canAddEntry) {
                        onAddEntry()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = if (canAddEntry) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Hinzufügen")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat(Constants.DATE_PATTERN, Locale.GERMANY)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        sdf.format(Date(it))
                    } ?: ""
                    if (selectedDate.isNotBlank()) {
                        onUpdateForm(selectedDate, null, null)
                        focusRequesterStunden.requestFocus()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun CreateInvoicePreview() {
    HonorarCraftAndroidTheme {
        CreateInvoiceContent(
            displayInvoiceNumber = "1",
            allInvoiceNumbers = listOf("1", "2"),
            invoiceFormat = InvoiceFormat.NUMBER,
            datum = "01.01.2024",
            stunden = "2.0",
            klasseFach = "Mathe",
            uniqueSubjects = listOf("Mathe", "Deutsch", "Englisch"),
            onUpdateForm = { _, _, _ -> },
            onAddEntry = { },
            onInvoiceSelect = {},
            onDeleteSubjectSuggestion = {},
            selectedTabIndex = 1,
            onTabSelected = {}
        )
    }
}
