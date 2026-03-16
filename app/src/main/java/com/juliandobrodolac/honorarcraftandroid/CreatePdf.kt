package com.juliandobrodolac.honorarcraftandroid

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun createPdf(
    context: Context,
    companyData: CompanyData,
    invoiceWithEntries: InvoiceWithEntries
) {
    val pdfDocument = PdfDocument()

    // Load saved format
    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val formatName = sharedPrefs.getString("invoice_format", InvoiceFormat.NUMBER.name)
        ?: InvoiceFormat.NUMBER.name
    val format = InvoiceFormat.valueOf(formatName)
    val displayInvoiceNumber = formatInvoice(invoiceWithEntries.invoice.invoiceNumber, format)

    // A4 size in points (72 DPI): 595 x 842
    val pageWidth = 595
    val pageHeight = 842

    val leftMargin = 71f   // 2,5 cm
    val rightMargin = 50f
    val topMargin = 71f    // 2,5 cm
    val bottomMargin = 71f // 2,5 cm

    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
    }
    val boldPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 11f
    }
    val textPaint = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = 11f
    }

    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    val today = dateFormatter.format(Date())

    // --- Page 1: Main Invoice ---
    val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page1 = pdfDocument.startPage(pageInfo1)
    val canvas1 = page1.canvas
    var y = topMargin

    // Helper for right-aligned text
    fun drawTextRight(text: String, yPos: Float, paint: Paint) {
        val width = paint.measureText(text)
        canvas1.drawText(text, pageWidth - rightMargin - width, yPos, paint)
    }

    // Sender Info (Right)
    drawTextRight("${companyData.billerFirstName} ${companyData.billerSecondName}", y, boldPaint)
    y += 15f
    drawTextRight("${companyData.billerStreetName} ${companyData.billerStreetNumber}", y, textPaint)
    y += 15f
    drawTextRight("${companyData.billerPlzNumber} ${companyData.billerCityName}", y, textPaint)
    y += 25f
    drawTextRight("Steuernummer: ${companyData.taxNumber}", y, textPaint)
    y += 15f
    drawTextRight("IBAN: ${companyData.billerIban}", y, textPaint)
    y += 15f
    drawTextRight("BIC: ${companyData.billerBIC}", y, textPaint)
    y += 25f

    val hasSignature =
        companyData.signaturePath.isNotBlank() && File(companyData.signaturePath).exists()

    // Rechnungsdatum nur anzeigen, wenn KEINE Unterschrift vorhanden ist
    if (!hasSignature) {
        drawTextRight("Rechnungsdatum: $today", y, textPaint)
    }

    // Title
    y = 250f
    canvas1.drawText("Honorarabrechnung", leftMargin, y, titlePaint)
    y += 35f

    // Invoice Number
    canvas1.drawText("Rechnung: $displayInvoiceNumber", leftMargin, y, boldPaint)
    y += 25f

    // Recipient Info
    canvas1.drawText(companyData.customerSecondNameOrOrga, leftMargin, y, boldPaint)
    y += 15f
    if (companyData.customerFirstName.isNotBlank()) {
        canvas1.drawText(companyData.customerFirstName, leftMargin, y, textPaint)
        y += 15f
    }
    if (companyData.customerMailBox.isNotBlank()) {
        canvas1.drawText("Postfach ${companyData.customerMailBox}", leftMargin, y, textPaint)
    } else {
        canvas1.drawText(
            "${companyData.customerStreet} ${companyData.customerStreetNumber}",
            leftMargin,
            y,
            textPaint
        )
    }
    y += 15f
    canvas1.drawText(
        "${companyData.customerPlz} ${companyData.customerCityName}",
        leftMargin,
        y,
        textPaint
    )
    y += 30f

    // Educational Center Details
    canvas1.drawText("Bildungszentrum: ${companyData.eduCenter}", leftMargin, y, textPaint)
    y += 15f
    canvas1.drawText("Standortnummer: ${companyData.locationNr}", leftMargin, y, textPaint)
    y += 15f
    canvas1.drawText("Schulart / Maßnahme: ${companyData.schoolType}", leftMargin, y, textPaint)
    y += 40f

    // Period and Summary
    val firstEntry = invoiceWithEntries.entries.firstOrNull()
    if (firstEntry != null) {
        val calendar = Calendar.getInstance()
        try {
            val date = dateFormatter.parse(firstEntry.date)
            if (date != null) calendar.time = date
        } catch (e: Exception) {
            // Keep current date if parsing fails
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartStr = dateFormatter.format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEndStr = dateFormatter.format(calendar.time)

        val introText = "Für den in der Zeit von $monthStartStr bis $monthEndStr " +
                "erteilten Fachunterricht und/oder andere Tätigkeiten gemäß meiner Aufstellung anbei," +
                " stelle ich wie folgt in Rechnung:"

        y = drawWrappedText(
            canvas1,
            introText,
            leftMargin,
            y,
            pageWidth - leftMargin - rightMargin,
            textPaint
        )
        y += 30f

        val totalUe = String.format(Locale.GERMAN, "%,.2f", invoiceWithEntries.totalLessonUnit)
        val totalSum = String.format(Locale.GERMAN, "%,.2f €", invoiceWithEntries.totalSum)
        val rate = String.format(Locale.GERMAN, "%,.2f", invoiceWithEntries.invoice.rate)

        canvas1.drawText(
            "UE Gesamt a 45 Min = $totalUe  x   $rate € Honorar/UE",
            leftMargin,
            y,
            boldPaint
        )
        y += 20f
        canvas1.drawText("Summe = $totalSum", leftMargin, y, boldPaint)
        y += 50f
    }

    // Unterschriftsbereich nur anzeigen, wenn Unterschrift vorhanden ist
    if (hasSignature) {
        val bitmap = BitmapFactory.decodeFile(companyData.signaturePath)
        if (bitmap != null) {
            val scaledW = 150f
            val scaledH = (bitmap.height.toFloat() / bitmap.width.toFloat()) * scaledW
            val destRect = android.graphics.RectF(leftMargin, y, leftMargin + scaledW, y + scaledH)
            canvas1.drawBitmap(bitmap, null, destRect, Paint())
            y += scaledH + 5f

            // Strich, Ort und Datum nur bei vorhandener Unterschrift
            canvas1.drawLine(
                leftMargin,
                y,
                leftMargin + 200f,
                y,
                Paint().apply { strokeWidth = 0.5f })
            y += 15f
            canvas1.drawText("${companyData.billerCityName}, $today", leftMargin, y, textPaint)
        }
    }

    pdfDocument.finishPage(page1)

    // --- Page 2+: Detailed Table ---
    if (invoiceWithEntries.entries.isNotEmpty()) {
        var pageNum = 2
        var tablePageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var tablePage = pdfDocument.startPage(tablePageInfo)
        var tableCanvas = tablePage.canvas
        val tableTop = topMargin
        val rowHeight = 20f
        var currentY = tableTop

        fun drawTableHeader(canvas: Canvas, yPos: Float) {
            canvas.drawText("Datum", leftMargin, yPos, boldPaint)
            canvas.drawText("UE", leftMargin + 60f, yPos, boldPaint)
            canvas.drawText("Summe", leftMargin + 110f, yPos, boldPaint)
            canvas.drawText("Unterrichtsfach/Klasse", leftMargin + 185f, yPos, boldPaint)
        }

        drawTableHeader(tableCanvas, currentY)
        currentY += rowHeight

        invoiceWithEntries.entries.forEachIndexed { index, entry ->
            if (currentY > pageHeight - bottomMargin - rowHeight) {
                pdfDocument.finishPage(tablePage)
                pageNum++
                tablePageInfo =
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                tablePage = pdfDocument.startPage(tablePageInfo)
                tableCanvas = tablePage.canvas
                currentY = tableTop
                drawTableHeader(tableCanvas, currentY)
                currentY += rowHeight
            }

            if (index % 2 == 1) {
                val rectPaint = Paint().apply { color = 0xFFF0F0F0.toInt() }
                tableCanvas.drawRect(
                    leftMargin,
                    currentY - 14f,
                    pageWidth - rightMargin,
                    currentY + 6f,
                    rectPaint
                )
            }

            val ue = entry.lessonUnits // No conversion needed anymore
            val cost = ue.multiply(invoiceWithEntries.invoice.rate).setScale(2, RoundingMode.HALF_UP)

            tableCanvas.drawText(entry.date, leftMargin, currentY, textPaint)
            tableCanvas.drawText(
                String.format(Locale.GERMAN, "%,.2f", ue),
                leftMargin + 60f,
                currentY,
                textPaint
            )
            tableCanvas.drawText(
                String.format(Locale.GERMAN, "%,.2f €", cost),
                leftMargin + 110f,
                currentY,
                textPaint
            )
            tableCanvas.drawText(entry.teachingSubject, leftMargin + 185f, currentY, textPaint)
            currentY += rowHeight
        }

        // Final summary on table page
        if (currentY > pageHeight - bottomMargin - 40f) {
            pdfDocument.finishPage(tablePage)
            pageNum++
            tablePageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            tablePage = pdfDocument.startPage(tablePageInfo)
            tableCanvas = tablePage.canvas
            currentY = tableTop
        }

        currentY += 20f
        tableCanvas.drawText(
            "UE Gesamt a 45 Min = ${
                String.format(
                    Locale.GERMAN,
                    "%,.2f",
                    invoiceWithEntries.totalLessonUnit
                )
            }", leftMargin, currentY, boldPaint
        )
        currentY += 15f
        tableCanvas.drawText(
            "Summe = ${
                String.format(
                    Locale.GERMAN,
                    "%,.2f €",
                    invoiceWithEntries.totalSum
                )
            }", leftMargin, currentY, boldPaint
        )

        pdfDocument.finishPage(tablePage)
    }

    // --- Save File to Public Documents ---
    // Use the same formatted number for the filename for consistency
    val fileName = "rechnung_${displayInvoiceNumber}.pdf"

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/HonorarCraft"
                )
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                Toast.makeText(
                    context,
                    "PDF in 'Dokumente/HonorarCraft' gespeichert",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                throw Exception("Konnte URI nicht erstellen")
            }
        } else {
            // Legacy for Android 9 and below
            val docsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(docsDir, "HonorarCraft")
            if (!appDir.exists()) appDir.mkdirs()

            val file = File(appDir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF gespeichert: ${file.absolutePath}", Toast.LENGTH_LONG)
                .show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Fehler beim Speichern: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    maxWidth: Float,
    paint: Paint
): Float {
    val words = text.split(" ")
    var line = ""
    var currentY = y
    for (word in words) {
        val testLine = if (line.isEmpty()) word else "$line $word"
        val width = paint.measureText(testLine)
        if (width > maxWidth) {
            canvas.drawText(line, x, currentY, paint)
            line = word
            currentY += paint.textSize + 4f
        } else {
            line = testLine
        }
    }
    canvas.drawText(line, x, currentY, paint)
    return currentY + paint.textSize
}
