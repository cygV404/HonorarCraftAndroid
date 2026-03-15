package com.juliandobrodolac.honorarcraftandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juliandobrodolac.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CreateInvoiceScreen(
    mainViewModel: MainViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val currentInvoiceNumber by mainViewModel.selectedInvoiceNumber.collectAsState()
    val allInvoiceNumbers by mainViewModel.allInvoiceNumbers.collectAsState()
    
    val datum by mainViewModel.currentDate.collectAsState()
    val stunden by mainViewModel.currentHours.collectAsState()
    val klasseFach by mainViewModel.currentSubject.collectAsState()
    val uniqueSubjects by mainViewModel.uniqueSubjects.collectAsState()
    
    CreateInvoiceContent(
        invoiceNumber = currentInvoiceNumber,
        allInvoiceNumbers = allInvoiceNumbers,
        datum = datum,
        stunden = stunden,
        klasseFach = klasseFach,
        uniqueSubjects = uniqueSubjects,
        onUpdateForm = { d, s, kf -> mainViewModel.updateInvoiceForm(d, s, kf) },
        onAddEntry = { mainViewModel.addEntryFromForm() },
        onInvoiceSelect = { mainViewModel.setSelectedInvoiceNumber(it) },
        onDeleteSubjectSuggestion = { mainViewModel.deleteSubjectSuggestion(it) },
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceContent(
    invoiceNumber: String,
    allInvoiceNumbers: List<String>,
    datum: String,
    stunden: String,
    klasseFach: String,
    uniqueSubjects: List<String>,
    onUpdateForm: (String?, String?, String?) -> Unit,
    onAddEntry: () -> Unit,
    onInvoiceSelect: (String) -> Unit,
    onDeleteSubjectSuggestion: (String) -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    val focusRequesterStunden = remember { FocusRequester() }
    val focusRequesterSubject = remember { FocusRequester() }

    var expandedSubject by remember { mutableStateOf(false) }
    
    // Filter logic: Show all if empty, otherwise filter by start. Sorted by frequency (handled by DAO).
    val filteredSubjects = if (klasseFach.isBlank()) {
        uniqueSubjects
    } else {
        uniqueSubjects.filter {
            it.startsWith(klasseFach, ignoreCase = true) && it.equals(klasseFach, ignoreCase = true).not()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Rechnung $invoiceNumber", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Optionen", modifier = Modifier.size(32.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        allInvoiceNumbers.forEach { num ->
                            DropdownMenuItem(
                                text = { Text("Rechnung $num") },
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
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)) {
                Column(
                    modifier = Modifier
                        .width(380.dp)
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center),
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
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Datum wählen")
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
                                    if (datum.isNotBlank() && stunden.isNotBlank()) {
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
                                                    onClick = { onDeleteSubjectSuggestion(selectionOption) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Löschen",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onUpdateForm(null, null, selectionOption)
                                            expandedSubject = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (datum.isNotBlank() && stunden.isNotBlank()) {
                        onAddEntry()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = if (datum.isNotBlank() && stunden.isNotBlank()) MaterialTheme.colorScheme.primary
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
                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
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
            invoiceNumber = "1",
            allInvoiceNumbers = listOf("1","2"),
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
