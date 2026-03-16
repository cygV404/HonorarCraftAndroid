package com.juliandobrodolac.honorarcraftandroid

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Represents the main invoice data stored in the 'invoices' table.
 */
@Entity(tableName = "invoices")
data class InvoiceData(
    @PrimaryKey val invoiceNumber: String,
    val rate: Double // Stored as Double for Room compatibility
)

/**
 * Represents the one-to-many relationship between an Invoice and its Entries.
 * This class is used to query an invoice with all its corresponding line items.
 */
data class InvoiceWithEntries(
    @Embedded val invoice: InvoiceData,
    @Relation(
        parentColumn = "invoiceNumber",
        entityColumn = "invoiceNumber"
    )
    val entries: List<InvoiceEntry>
) {
    // --- Business Logic from the original class, adapted for Double ---

    private fun calculateCorrectedHours(units: Double): Double {
        return (units * 60) / 45
    }


    val totalSum: Double
        get() = entries.sumOf { entry ->
            val ue = calculateCorrectedHours(entry.lessonUnits)
            ue * invoice.rate
        }


    val totalLessonUnit: Double
        get() = entries.sumOf { calculateCorrectedHours(it.lessonUnits) }

}
