package com.juliandobrodolac.honorarcraftandroid

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_entries")
data class InvoiceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var invoiceNumber: String, // Foreign key to link to an InvoiceData
    val date: String,
    val lessonUnits: Double,
    val teachingSubject: String
)
