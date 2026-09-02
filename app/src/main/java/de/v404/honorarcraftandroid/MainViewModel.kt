package de.v404.honorarcraftandroid

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "MainViewModel"

enum class InvoiceFormat {
    NUMBER,
    YEAR_NUMBER,
    YEAR_MONTH_NUMBER
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val invoiceDao = database.invoiceDao()
    private val companyDao = database.companyDao()
    private val sharedPrefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Navigation State
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _pendingTabIndex = MutableStateFlow<Int?>(null)
    val pendingTabIndex: StateFlow<Int?> = _pendingTabIndex.asStateFlow()

    // Trigger to reset UI state in DataWindow (increments to force a discard)
    private val _resetDataWindowTrigger = MutableStateFlow(0)
    val resetDataWindowTrigger: StateFlow<Int> = _resetDataWindowTrigger.asStateFlow()

    // Dashboard Year (for Revenue display)
    private val _dashboardYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val dashboardYear: StateFlow<Int> = _dashboardYear.asStateFlow()

    // Selection for Year and Invoice (for Invoice Number generation)
    private val _invoiceYear = MutableStateFlow(
        sharedPrefs.getInt("invoice_year", Calendar.getInstance().get(Calendar.YEAR))
    )
    val invoiceYear: StateFlow<Int> = _invoiceYear.asStateFlow()

    private val _invoiceMonth = MutableStateFlow(
        sharedPrefs.getInt("invoice_month", Calendar.getInstance().get(Calendar.MONTH) + 1)
    )
    val invoiceMonth: StateFlow<Int> = _invoiceMonth.asStateFlow()

    private val _selectedInvoiceNumber = MutableStateFlow(
        sharedPrefs.getString("selected_invoice_number", "1") ?: "1"
    )
    val selectedInvoiceNumber: StateFlow<String> = _selectedInvoiceNumber.asStateFlow()

    private val _invoiceFormat = MutableStateFlow(
        runCatching {
            InvoiceFormat.valueOf(
                sharedPrefs.getString("invoice_format", InvoiceFormat.NUMBER.name)
                    ?: InvoiceFormat.NUMBER.name
            )
        }.getOrDefault(InvoiceFormat.NUMBER)
    )
    val invoiceFormat: StateFlow<InvoiceFormat> = _invoiceFormat.asStateFlow()

    val formattedInvoiceNumber: StateFlow<String> = combine(
        _selectedInvoiceNumber,
        _invoiceFormat,
        _invoiceYear,
        _invoiceMonth
    ) { number, format, year, month ->
        formatInvoice(number, format, year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        formatInvoice(
            _selectedInvoiceNumber.value,
            _invoiceFormat.value,
            _invoiceYear.value,
            _invoiceMonth.value
        )
    )

    // Form State for CreateInvoice
    private val _currentDate = MutableStateFlow(getCurrentDateFormatted())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _currentHours = MutableStateFlow("")
    val currentHours: StateFlow<String> = _currentHours.asStateFlow()

    private val _currentSubject = MutableStateFlow("")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _lastAddedEntry = MutableStateFlow<InvoiceEntry?>(null)
    val lastAddedEntry: StateFlow<InvoiceEntry?> = _lastAddedEntry.asStateFlow()

    private val _showEntryConfirmation = MutableStateFlow(false)
    val showEntryConfirmation: StateFlow<Boolean> = _showEntryConfirmation.asStateFlow()

    private var confirmationJob: Job? = null

    // Yearly Revenue - linked to _dashboardYear
    val yearlyRevenue: StateFlow<BigDecimal> = _dashboardYear
        .flatMapLatest { year ->
            invoiceDao.getInvoicesWithEntriesByYear(year.toString())
                .map { invoices -> year to invoices }
        }
        .map { (year, invoices) ->
            invoices.fold(BigDecimal.ZERO) { acc, invoiceWithEntries ->
                val yearStr = year.toString()
                val entriesForYear = invoiceWithEntries.entries.filter { it.date.endsWith(yearStr) }
                
                val sumForYear = entriesForYear.fold(BigDecimal.ZERO) { entryAcc, entry ->
                    val ue = entry.lessonUnits.multiply(BigDecimal("60"))
                        .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
                    entryAcc.add(ue.multiply(entry.rate))
                }
                acc.add(sumForYear)
            }.setScale(2, RoundingMode.HALF_UP)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    // Company Data
    val companyData: StateFlow<CompanyData?> = companyDao.getCompanyData()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Current Invoice with Entries - reactive to formattedInvoiceNumber
    val currentInvoiceWithEntries: StateFlow<InvoiceWithEntries?> = formattedInvoiceNumber
        .flatMapLatest { number -> invoiceDao.getInvoiceWithEntries(number) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All available invoice numbers for dropdowns
    val allInvoiceNumbers: StateFlow<List<String>> = invoiceDao.getAllInvoiceNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique subjects for suggestions
    val uniqueSubjects: StateFlow<List<String>> = invoiceDao.getUniqueSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat(Constants.DATE_PATTERN, Locale.GERMANY)
        return sdf.format(Date())
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun requestTabChange(index: Int) {
        if (_hasUnsavedChanges.value && _selectedTabIndex.value == 3 && index != 3) {
            _pendingTabIndex.value = index
        } else {
            _selectedTabIndex.value = index
            _pendingTabIndex.value = null
        }
    }

    fun updateTabIndexFromPager(index: Int) {
        if (_selectedTabIndex.value != index) {
            _selectedTabIndex.value = index
        }
    }

    fun confirmTabChange() {
        _pendingTabIndex.value?.let { target ->
            _hasUnsavedChanges.value = false
            _resetDataWindowTrigger.value += 1
            _selectedTabIndex.value = target
            _pendingTabIndex.value = null
        }
    }

    fun cancelTabChange() {
        _pendingTabIndex.value = null
    }

    fun setHasUnsavedChanges(hasChanges: Boolean) {
        _hasUnsavedChanges.value = hasChanges
    }

    fun setDashboardYear(year: Int) {
        _dashboardYear.value = year
    }

    fun setInvoiceYear(year: Int) {
        _invoiceYear.value = year
        sharedPrefs.edit().putInt("invoice_year", year).apply()
    }

    fun setInvoiceMonth(month: Int) {
        _invoiceMonth.value = month
        sharedPrefs.edit().putInt("invoice_month", month).apply()
    }

    fun setSelectedInvoiceNumber(number: String) {
        // Check if it's a composite number like "2026-01-01" or "2026-01"
        val parts = number.split("-")
        if (parts.size >= 2) {
            val yearPart = parts[0].toIntOrNull()
            val monthOrNumPart = parts[1].toIntOrNull()
            
            if (yearPart != null) _invoiceYear.value = yearPart
            
            if (parts.size == 3) {
                // Format: YEAR-MONTH-NUMBER
                if (monthOrNumPart != null) _invoiceMonth.value = monthOrNumPart
                val numPart = parts[2].toIntOrNull()
                val formattedNum = if (numPart != null) String.format(Locale.GERMANY, "%02d", numPart) else parts[2]
                _selectedInvoiceNumber.value = formattedNum
            } else if (parts.size == 2 && invoiceFormat.value == InvoiceFormat.YEAR_NUMBER) {
                // Format: YEAR-NUMBER
                val numPart = parts[1].toIntOrNull()
                val formattedNum = if (numPart != null) String.format(Locale.GERMANY, "%02d", numPart) else parts[1]
                _selectedInvoiceNumber.value = formattedNum
            } else {
                // Fallback for simple number or unknown format
                val numInt = number.toIntOrNull()
                val formatted = if (numInt != null) String.format(Locale.GERMANY, "%02d", numInt) else number
                _selectedInvoiceNumber.value = formatted
            }
        } else {
            val numInt = number.toIntOrNull()
            val formatted = if (numInt != null) String.format(Locale.GERMANY, "%02d", numInt) else number
            _selectedInvoiceNumber.value = formatted
        }
        
        sharedPrefs.edit().putString("selected_invoice_number", _selectedInvoiceNumber.value).apply()
        
        // Reset confirmation card to avoid "ghost" entries from previous invoice
        _lastAddedEntry.value = null
        _showEntryConfirmation.value = false
    }

    fun setInvoiceFormat(format: InvoiceFormat) {
        _invoiceFormat.value = format
        sharedPrefs.edit().putString("invoice_format", format.name).apply()
    }

    fun incrementInvoiceNumber() {
        val current = _selectedInvoiceNumber.value.toIntOrNull() ?: 0
        setSelectedInvoiceNumber((current + 1).toString())
    }

    fun updateInvoiceForm(date: String? = null, hours: String? = null, subject: String? = null) {
        date?.let { _currentDate.value = it }
        hours?.let { _currentHours.value = it }
        subject?.let { _currentSubject.value = it }
    }

    fun saveCompanyData(data: CompanyData) {
        viewModelScope.launch {
            _isLoading.value = true
            companyDao.insertCompanyData(data)
            _isLoading.value = false
            _hasUnsavedChanges.value = false
        }
    }

    /**
     * Blendet einen Fachvorschlag aus der Vorschlagsliste aus.
     *
     * Löscht bewusst keine Rechnungspositionen: der Vorschlag verschwindet, die
     * gebuchten Belege bleiben. Wird das Fach später wieder gebucht, taucht der
     * Vorschlag automatisch wieder auf (siehe [addEntryFromForm]).
     */
    fun hideSubjectSuggestion(subject: String) {
        viewModelScope.launch {
            invoiceDao.hideSubject(HiddenSubject(subject.trim()))
        }
    }

    fun addEntryFromForm() {
        // Durchgaengig getrimmt: ein unsichtbares Leerzeichen im Stundenfeld liess
        // toBigDecimalOrNull() zuvor scheitern ("Ungültige Stundenzahl", obwohl das
        // Feld gueltig aussah), im Fachfeld erzeugte es doppelte Vorschlaege, und im
        // Datumsfeld haette es date.endsWith(jahr) und damit den Jahresumsatz gekippt.
        val datum = _currentDate.value.trim()
        val stunden = _currentHours.value.trim()
        val klasseFach = _currentSubject.value.trim()

        if (datum.isBlank() || stunden.isBlank()) return

        val number = formattedInvoiceNumber.value
        val hours = stunden.replace(",", ".").toBigDecimalOrNull()

        if (hours == null) {
            Toast.makeText(getApplication(), "Ungültige Stundenzahl", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val existingInvoice = invoiceDao.getInvoice(number)
            val cd = companyDao.getCompanyData().first()
            val rate = cd?.rate ?: Constants.DEFAULT_RATE

            if (existingInvoice == null) {
                invoiceDao.insertInvoice(InvoiceData(invoiceNumber = number))
            }

            val entry = InvoiceEntry(
                invoiceNumber = number,
                date = datum,
                lessonUnits = hours,
                teachingSubject = klasseFach,
                rate = rate
            )
            invoiceDao.insertEntry(entry)
            // Wer ein Fach wieder bucht, will es auch wieder vorgeschlagen bekommen.
            if (klasseFach.isNotBlank()) invoiceDao.unhideSubject(klasseFach)

            confirmationJob?.cancel()
            _lastAddedEntry.value = entry
            _showEntryConfirmation.value = true

            _currentHours.value = ""
            _currentSubject.value = ""
            _currentDate.value = getCurrentDateFormatted()

            confirmationJob = launch {
                delay(4500)  // Delay für 4.5 Sekunden
                _showEntryConfirmation.value = false
            }
        }
    }

    fun deleteEntries(entries: List<InvoiceEntry>) {
        viewModelScope.launch {
            invoiceDao.deleteEntries(entries)
        }
    }

    fun deleteInvoice(invoiceNumber: String) {
        viewModelScope.launch {
            invoiceDao.deleteInvoice(InvoiceData(invoiceNumber))
            if (_selectedInvoiceNumber.value == invoiceNumber) {
                setSelectedInvoiceNumber("1")
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    sharedPrefs.edit().clear().commit()
                }

                // Reset StateFlows to default values
                val now = Calendar.getInstance()
                _dashboardYear.value = now.get(Calendar.YEAR)
                _invoiceYear.value = now.get(Calendar.YEAR)
                _invoiceMonth.value = now.get(Calendar.MONTH) + 1
                _selectedInvoiceNumber.value = "1"
                _invoiceFormat.value = InvoiceFormat.NUMBER

                _hasUnsavedChanges.value = false
                _resetDataWindowTrigger.value += 1
            } catch (e: Exception) {
                Log.e(TAG, "Zurücksetzen fehlgeschlagen", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Fehler beim Zurücksetzen: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetCompanyData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    companyDao.insertCompanyData(CompanyData(id = 1, rate = Constants.DEFAULT_RATE))
                }
                _hasUnsavedChanges.value = false
                _resetDataWindowTrigger.value += 1
            } catch (e: Exception) {
                Log.e(TAG, "Zurücksetzen fehlgeschlagen", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Fehler beim Zurücksetzen: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Schreibt die Datenbank in die vom Nutzer gewaehlte Datei. */
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val ergebnis = Backup.export(getApplication(), uri)
            _isLoading.value = false
            ergebnis.onSuccess {
                Toast.makeText(getApplication(), "Sicherung gespeichert", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(
                    getApplication(),
                    "Sicherung fehlgeschlagen: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Ersetzt die Datenbank durch eine Sicherung und startet die App neu.
     *
     * Der Neustart ist nicht Bequemlichkeit, sondern notwendig: saemtliche
     * StateFlows haengen an der Room-Verbindung, die der Import schliesst. Ein
     * Weiterlaufen ohne Neustart wuerde leere oder veraltete Listen zeigen.
     */
    fun importData(uri: Uri, onRestart: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val ergebnis = Backup.import(getApplication(), uri)
            _isLoading.value = false
            ergebnis.onSuccess {
                Toast.makeText(
                    getApplication(),
                    "Sicherung eingelesen – App wird neu gestartet",
                    Toast.LENGTH_LONG
                ).show()
                onRestart()
            }.onFailure { fehler ->
                Toast.makeText(
                    getApplication(),
                    fehler.message ?: "Import fehlgeschlagen",
                    Toast.LENGTH_LONG
                ).show()
                // Scheiterte der Tausch erst nach dem Schliessen der Verbindung,
                // sind die Daten zwar heil, die laufende App aber nicht mehr
                // benutzbar - also ebenfalls neu starten.
                if (fehler is NeustartNoetigException) onRestart()
            }
        }
    }

    fun generatePdf(context: Context, companyData: CompanyData, invoiceWithEntries: InvoiceWithEntries) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val formattedNumber = formattedInvoiceNumber.value
                val success =
                    createInvoicePdf(context, invoiceWithEntries, companyData, formattedNumber) { _ -> }
                if (success) {
                    incrementInvoiceNumber()
                }
            } catch (t: Throwable) {
                // Fängt bewusst auch Error (z. B. OutOfMemoryError beim Zeichnen):
                // eine ungefangene Exception in viewModelScope beendet sonst den Prozess.
                Log.e(TAG, "PDF-Erzeugung fehlgeschlagen", t)
                Toast.makeText(
                    getApplication(),
                    "PDF konnte nicht erstellt werden",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }
}

fun formatInvoice(number: String, format: InvoiceFormat, year: Int, month: Int): String {
    val numInt = number.toIntOrNull()
    val displayNum = if (numInt != null) String.format(Locale.GERMANY, "%02d", numInt) else number
    
    return when (format) {
        InvoiceFormat.NUMBER -> displayNum
        InvoiceFormat.YEAR_NUMBER -> "$year-$displayNum"
        InvoiceFormat.YEAR_MONTH_NUMBER -> String.format(
            Locale.GERMANY,
            "%d-%02d-%02d",
            year,
            month,
            numInt ?: 0
        )
    }
}
