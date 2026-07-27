package de.v404.honorarcraftandroid

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import de.v404.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun DataWindowScreen(
    mainViewModel: MainViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val savedData by mainViewModel.companyData.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()
    val resetTrigger by mainViewModel.resetDataWindowTrigger.collectAsState()

    // key(resetTrigger) sorgt dafür, dass die gesamte Content-Composable neu initialisiert wird,
    // wenn "Verwerfen" geklickt wird oder die Daten hart zurückgesetzt werden.
    key(resetTrigger) {
        DataWindowContent(
            savedData = savedData,
            isLoading = isLoading,
            onSave = { mainViewModel.saveCompanyData(it) },
            onResetAll = { mainViewModel.resetAllData() },
            onResetCompanyOnly = { mainViewModel.resetCompanyData() },
            onChanged = { mainViewModel.setHasUnsavedChanges(true) },
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataWindowContent(
    savedData: CompanyData?,
    isLoading: Boolean,
    onSave: (CompanyData) -> Unit,
    onResetAll: () -> Unit,
    onResetCompanyOnly: () -> Unit,
    onChanged: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current

    // Initialisierung des lokalen Zustands mit den Daten aus der Datenbank.
    // Da diese Composable durch key(resetTrigger) neu erstellt wird, 
    // greift remember hier jedes Mal neu mit den Originaldaten.
    var companyDataState by remember(savedData) {
        mutableStateOf(savedData?.copy() ?: CompanyData())
    }

    var showMenu by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }
    var showResetCompanyDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream == null) {
                    Toast.makeText(context, "Datei konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
                    return@let
                }
                val file = File(context.filesDir, "signature.png")
                FileOutputStream(file).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                companyDataState = companyDataState.copy(signaturePath = file.absolutePath)
                onChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Fehler beim Kopieren der Signatur", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialog für ALLES zurücksetzen
    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Werkseinstellungen") },
            text = {
                Text(
                    "Möchten Sie wirklich alle Daten zurücksetzen? " +
                            "Dabei werden alle Kundendaten, Rechnungen und Rechnungsnummern gelöscht. " +
                            "Bereits generierte PDF-Dateien bleiben davon unberührt."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetAll()
                        showResetAllDialog = false
                    }
                ) {
                    Text("Alles löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Dialog für nur Datenfelder zurücksetzen
    if (showResetCompanyDialog) {
        AlertDialog(
            onDismissRequest = { showResetCompanyDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Datenfelder leeren") },
            text = {
                Text(
                    "Möchten Sie nur die persönlichen Datenfelder und Firmendaten leeren? " +
                            "Ihre Rechnungen und Einträge bleiben erhalten."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetCompanyOnly()
                        showResetCompanyDialog = false
                    }
                ) {
                    Text("Felder leeren", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCompanyDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Daten", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            actions = {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.Settings, contentDescription = "Menü anzeigen")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Datenfelder leeren") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showResetCompanyDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Werkseinstellungen") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showResetAllDialog = true
                        }
                    )
                }
            }
        )

        Box(modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionHeader("Kundendaten") }
                item {
                    DataField(
                        label = "Bildungszentrum",
                        value = companyDataState.eduCenter,
                        onValueChange = {
                            companyDataState = companyDataState.copy(eduCenter = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Standort Nr",
                        value = companyDataState.locationNr,
                        onValueChange = {
                            companyDataState = companyDataState.copy(locationNr = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Schulart/Maßnahme",
                        value = companyDataState.schoolType,
                        onValueChange = {
                            companyDataState = companyDataState.copy(schoolType = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Name/Orga",
                        value = companyDataState.customerSecondNameOrOrga,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerSecondNameOrOrga = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Vorname",
                        value = companyDataState.customerFirstName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerFirstName = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Straße",
                        value = companyDataState.customerStreet,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerStreet = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Hausnummer",
                        value = companyDataState.customerStreetNumber,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerStreetNumber = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "PLZ",
                        value = companyDataState.customerPlz,
                        onValueChange = {
                            companyDataState = companyDataState.copy(customerPlz = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Postfach",
                        value = companyDataState.customerMailBox,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerMailBox = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Ort",
                        value = companyDataState.customerCityName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(customerCityName = it); onChanged()
                        })
                }

                item { SectionHeader("Meine Daten") }
                item {
                    DataField(
                        label = "Name",
                        value = companyDataState.billerSecondName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerSecondName = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Vorname",
                        value = companyDataState.billerFirstName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerFirstName = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Straße",
                        value = companyDataState.billerStreetName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerStreetName = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Hausnummer",
                        value = companyDataState.billerStreetNumber,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerStreetNumber = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "PLZ",
                        value = companyDataState.billerPlzNumber,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerPlzNumber = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Ort",
                        value = companyDataState.billerCityName,
                        onValueChange = {
                            companyDataState =
                                companyDataState.copy(billerCityName = it); onChanged()
                        })
                }

                // Signature Picker
                item {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)) {
                        Text(
                            "Unterschrift (PNG)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.extraSmall
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { launcher.launch("image/png") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (companyDataState.signaturePath.isNotEmpty() && File(companyDataState.signaturePath).exists()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(companyDataState.signaturePath)),
                                        contentDescription = "Unterschrift Vorschau",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Delete/Clear Button
                                    IconButton(
                                        onClick = {
                                            companyDataState =
                                                companyDataState.copy(signaturePath = "")
                                            onChanged()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Unterschrift entfernen",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "PNG auswählen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionHeader("Finanzdaten") }
                item {
                    DataField(
                        label = "Steuernummer",
                        value = companyDataState.taxNumber,
                        onValueChange = {
                            companyDataState = companyDataState.copy(taxNumber = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "IBAN",
                        value = companyDataState.billerIban,
                        onValueChange = {
                            companyDataState = companyDataState.copy(billerIban = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "BIC",
                        value = companyDataState.billerBIC,
                        onValueChange = {
                            companyDataState = companyDataState.copy(billerBIC = it); onChanged()
                        })
                }
                item {
                    DataField(
                        label = "Honorarsatz €",
                        value = companyDataState.rate,
                        onValueChange = {
                            companyDataState = companyDataState.copy(rate = it); onChanged()
                        })
                }
            }

            FloatingActionButton(
                onClick = { if (!isLoading) onSave(companyDataState) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = if (isLoading) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Speichern")
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .pointerInput(Unit) { },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 8.dp),
        style = TextStyle(
            fontWeight = FontWeight(600),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun DataField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun DataWindowPreview() {
    HonorarCraftAndroidTheme {
        DataWindowContent(
            savedData = CompanyData(),
            isLoading = false,
            onSave = {},
            onResetAll = {},
            onResetCompanyOnly = {},
            onChanged = {},
            selectedTabIndex = 3,
            onTabSelected = {}
        )
    }
}
