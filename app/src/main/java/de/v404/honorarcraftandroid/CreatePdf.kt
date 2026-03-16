package de.v404.honorarcraftandroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createInvoicePdf(
    context: Context,
    invoiceWithEntries: InvoiceWithEntries,
    companyData: CompanyData?,
    onFinished: (File) -> Unit
) {
    if (companyData == null) {
        Toast.makeText(context, "Unternehmensdaten fehlen!", Toast.LENGTH_SHORT).show()
        return
    }

    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()
    val titlePaint = Paint()

    val margin = 50f
    var yPos = 80f

    // Biller Info (Top Right)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 10f
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${companyData.billerFirstName} ${companyData.billerSecondName}", 545f, yPos, paint)
    yPos += 15f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("${companyData.billerStreetName} ${companyData.billerStreetNumber}", 545f, yPos, paint)
    yPos += 15f
    canvas.drawText("${companyData.billerPlzNumber} ${companyData.billerCityName}", 545f, yPos, paint)
    yPos += 30f

    // Customer Info (Left)
    yPos = 160f
    paint.textAlign = Paint.Align.LEFT
    paint.textSize = 10f
    canvas.drawText(companyData.customerSecondNameOrOrga, margin, yPos, paint)
    yPos += 15f
    if (companyData.customerFirstName.isNotEmpty()) {
        canvas.drawText(companyData.customerFirstName, margin, yPos, paint)
        yPos += 15f
    }
    canvas.drawText("${companyData.customerStreet} ${companyData.customerStreetNumber}", margin, yPos, paint)
    yPos += 15f
    canvas.drawText("${companyData.customerPlz} ${companyData.customerCityName}", margin, yPos, paint)

    // Invoice Header
    yPos = 280f
    titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    titlePaint.textSize = 18f
    canvas.drawText("Rechnung", margin, yPos, titlePaint)

    yPos += 40f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 10f
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
    canvas.drawText("Rechnungsnummer: ${invoiceWithEntries.invoice.invoiceNumber}", margin, yPos, paint)
    canvas.drawText("Datum: $dateStr", 545f, yPos, Paint(paint).apply { textAlign = Paint.Align.RIGHT })

    yPos += 40f
    // Table Header
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Datum", margin, yPos, paint)
    canvas.drawText("Beschreibung", 130f, yPos, paint)
    canvas.drawText("U-Std", 400f, yPos, paint)
    canvas.drawText("Betrag", 545f, yPos, Paint(paint).apply { textAlign = Paint.Align.RIGHT })

    yPos += 5f
    canvas.drawLine(margin, yPos, 545f, yPos, paint)
    yPos += 20f

    // Table Content
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val rate = invoiceWithEntries.invoice.rate

    invoiceWithEntries.entries.forEach { entry ->
        canvas.drawText(entry.date, margin, yPos, paint)
        canvas.drawText(entry.teachingSubject, 130f, yPos, paint)
        canvas.drawText("%.2f".format(entry.lessonUnits), 400f, yPos, paint)

        val amount = entry.lessonUnits.multiply(BigDecimal("60"))
            .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
            .multiply(rate)
        canvas.drawText("${"%.2f".format(amount)} €", 545f, yPos, Paint(paint).apply { textAlign = Paint.Align.RIGHT })
        yPos += 20f
    }

    yPos += 10f
    canvas.drawLine(margin, yPos, 545f, yPos, paint)
    yPos += 25f

    // Total
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    canvas.drawText("Gesamtbetrag:", 400f, yPos, Paint(paint).apply { textAlign = Paint.Align.RIGHT })
    canvas.drawText("${"%.2f".format(invoiceWithEntries.totalSum)} €", 545f, yPos, Paint(paint).apply { textAlign = Paint.Align.RIGHT })

    // Footer
    yPos = 750f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 8f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Steuernummer: ${companyData.taxNumber} | IBAN: ${companyData.billerIban} | BIC: ${companyData.billerBIC}", 297.5f, yPos, paint)

    pdfDocument.finishPage(page)

    val file = File(context.getExternalFilesDir(null), "Rechnung_${invoiceWithEntries.invoice.invoiceNumber}.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        onFinished(file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    pdfDocument.close()
}
