package com.juliandobrodolac.honorarcraftandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val invoiceDao = database.invoiceDao()
    private val companyDao = database.companyDao()

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

    // Selection for Year and Invoice
    private val _selectedYear = MutableStateFlow(2026)
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedInvoiceNumber = MutableStateFlow("1")
    val selectedInvoiceNumber: StateFlow<String> = _selectedInvoiceNumber

    // Form State for CreateInvoice
    private val _currentDate = MutableStateFlow(getCurrentDateFormatted())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _currentHours = MutableStateFlow("")
    val currentHours: StateFlow<String> = _currentHours.asStateFlow()

    private val _currentSubject = MutableStateFlow("")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    // Yearly Revenue - reactive to _selectedYear
    val yearlyRevenue: StateFlow<Double> = _selectedYear
        .flatMapLatest { year -> 
            invoiceDao.getRevenueForYear(year.toString()) 
        }
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Company Data
    val companyData: StateFlow<CompanyData?> = companyDao.getCompanyData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Invoice with Entries - reactive to _selectedInvoiceNumber
    val currentInvoiceWithEntries: StateFlow<InvoiceWithEntries?> = _selectedInvoiceNumber
        .flatMapLatest { number -> invoiceDao.getInvoiceWithEntries(number) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All available invoice numbers for dropdowns
    val allInvoiceNumbers: StateFlow<List<String>> = invoiceDao.getAllInvoiceNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique subjects for suggestions
    val uniqueSubjects: StateFlow<List<String>> = invoiceDao.getUniqueSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        return sdf.format(Date())
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setSelectedTabIndex(index: Int) {
        // If we are on the Data tab (index 3) and have unsaved changes, and want to navigate away
        if (_hasUnsavedChanges.value && _selectedTabIndex.value == 3 && index != 3) {
            _pendingTabIndex.value = index
        } else {
            _selectedTabIndex.value = index
            _pendingTabIndex.value = null
        }
    }

    fun confirmTabChange() {
        _pendingTabIndex.value?.let { target ->
            // First increment trigger to signal discard
            _resetDataWindowTrigger.value += 1
            // Clear changes flag
            _hasUnsavedChanges.value = false
            // Navigate
            _selectedTabIndex.value = target
            // Clear pending
            _pendingTabIndex.value = null
        }
    }

    fun cancelTabChange() {
        _pendingTabIndex.value = null
    }

    fun setHasUnsavedChanges(hasChanges: Boolean) {
        _hasUnsavedChanges.value = hasChanges
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    fun setSelectedInvoiceNumber(number: String) {
        _selectedInvoiceNumber.value = number
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
            delay(1000) // Small delay to show the animation
            _isLoading.value = false
            _hasUnsavedChanges.value = false
        }
    }

    fun deleteSubjectSuggestion(subject: String) {
        viewModelScope.launch {
            invoiceDao.deleteEntriesBySubject(subject)
        }
    }

    fun addEntryFromForm() {
        val datum = _currentDate.value
        val stunden = _currentHours.value
        val klasseFach = _currentSubject.value
        
        if (datum.isBlank() || stunden.isBlank()) return

        val number = _selectedInvoiceNumber.value
        val hours = stunden.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val existingInvoice = invoiceDao.getInvoice(number)

            if (existingInvoice == null) {
                val rate = companyData.value?.rate?.toDoubleOrNull() ?: 23.0
                invoiceDao.insertInvoice(InvoiceData(invoiceNumber = number, rate = rate))
            }

            val entry = InvoiceEntry(
                invoiceNumber = number,
                date = datum,
                lessonUnits = hours,
                teachingSubject = klasseFach
            )
            invoiceDao.insertEntry(entry)
            
            _currentHours.value = ""
            _currentSubject.value = ""
            _currentDate.value = getCurrentDateFormatted()
        }
    }

    fun addEntry(datum: String, stunden: String, klasseFach: String) {
        val number = _selectedInvoiceNumber.value
        val hours = stunden.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val existingInvoice = invoiceDao.getInvoice(number)

            if (existingInvoice == null) {
                val rate = companyData.value?.rate?.toDoubleOrNull() ?: 23.0
                invoiceDao.insertInvoice(InvoiceData(invoiceNumber = number, rate = rate))
            }

            val entry = InvoiceEntry(
                invoiceNumber = number,
                date = datum,
                lessonUnits = hours,
                teachingSubject = klasseFach
            )
            invoiceDao.insertEntry(entry)
        }
    }

    fun deleteEntries(entries: List<InvoiceEntry>) {
        viewModelScope.launch {
            invoiceDao.deleteEntries(entries)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            database.clearAllTables()
            delay(1000)
            _isLoading.value = false
            _hasUnsavedChanges.value = false
            _resetDataWindowTrigger.value += 1
        }
    }

    fun resetCompanyData() {
        viewModelScope.launch {
            _isLoading.value = true
            companyDao.insertCompanyData(CompanyData(id = 1))
            delay(500)
            _isLoading.value = false
            _hasUnsavedChanges.value = false
            _resetDataWindowTrigger.value += 1
        }
    }
}
