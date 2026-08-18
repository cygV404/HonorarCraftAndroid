package de.v404.honorarcraftandroid

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Keep
@Entity(
    tableName = "invoice_entries",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceData::class,
            parentColumns = ["invoiceNumber"],
            childColumns = ["invoiceNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoiceNumber"]),
        Index(value = ["teachingSubject"]),
        Index(value = ["date"])
    ]
)
data class InvoiceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var invoiceNumber: String, // Foreign key to link to an InvoiceData
    val date: String,
    val lessonUnits: BigDecimal,
    val teachingSubject: String,
    val rate: BigDecimal = BigDecimal("23.0")
)
