package de.v404.honorarcraftandroid

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents the main invoice data stored in the 'invoices' table.
 */
@Entity(tableName = "invoices")
data class InvoiceData(
    @PrimaryKey val invoiceNumber: String,
    val rate: BigDecimal
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
    val totalSum: BigDecimal
        get() = entries.fold(BigDecimal.ZERO) { acc, entry ->
            val ueValue = entry.lessonUnits.multiply(BigDecimal("60"))
                .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
            acc.add(ueValue.multiply(invoice.rate))
        }.setScale(2, RoundingMode.HALF_UP)


    val totalLessonUnit: BigDecimal
        get() = entries.fold(BigDecimal.ZERO) { acc, entry ->
            val ueValue = entry.lessonUnits.multiply(BigDecimal("60"))
                .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
            acc.add(ueValue)
        }.setScale(2, RoundingMode.HALF_UP)

}
