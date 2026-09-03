package de.v404.honorarcraftandroid

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "CreatePdf"

/** Zielbreite der Unterschrift: 150 pt Darstellung bei 3-facher Auflösung. */
private const val SIGNATURE_MAX_WIDTH_PX = 450

suspend fun createInvoicePdf(
    context: Context,
    invoiceWithEntries: InvoiceWithEntries,
    companyData: CompanyData?,
    formattedInvoiceNumber: String,
    onFinished: (File?) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    if (companyData == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Unternehmensdaten fehlen!", Toast.LENGTH_SHORT).show()
        }
        return@withContext false
    }

    val pdfDocument = PdfDocument()
    // Ausserhalb des try, damit das finally sie freigeben kann.
    var signatureBitmap: Bitmap? = null
    try {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val contentWidth = pageWidth - 2 * margin

        val paint = Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val paintBold = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val dateFormatter = SimpleDateFormat(Constants.DATE_PATTERN, Locale.GERMANY)
        val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY))
        val today = dateFormatter.format(Date())

        // Unterschrift laden. Bewusst herunterskaliert: das Bild wird auf 150 x 50 pt
        // gezeichnet, ein Kamerafoto in voller Auflösung würde den Heap sprengen.
        // catch(Throwable), weil OutOfMemoryError ein Error und keine Exception ist.
        signatureBitmap = companyData.signaturePath
            .takeIf { it.isNotBlank() && File(it).exists() }
            ?.let { path ->
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    var sample = 1
                    while (bounds.outWidth > 0 && bounds.outWidth / sample > SIGNATURE_MAX_WIDTH_PX) {
                        sample *= 2
                    }
                    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                        inSampleSize = sample
                    })
                } catch (t: Throwable) {
                    Log.e(TAG, "Unterschrift konnte nicht geladen werden", t)
                    null
                }
            }
        val showSignature = signatureBitmap != null

        // --- Seite 1: Deckblatt ---
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 80f

        // Absender (Rechtsbündig)
        paint.textAlign = Paint.Align.RIGHT
        paintBold.textAlign = Paint.Align.RIGHT
        val rightPos = pageWidth - margin

        canvas.drawText("${companyData.billerSecondName} ${companyData.billerFirstName}", rightPos, y, paintBold)
        y += 15f
        canvas.drawText("${companyData.billerStreetName} ${companyData.billerStreetNumber}", rightPos, y, paint)
        y += 15f
        canvas.drawText("${companyData.billerPlzNumber} ${companyData.billerCityName}", rightPos, y, paint)
        y += 30f
    
        if (companyData.taxNumber.isNotBlank()) {
            canvas.drawText("Steuernummer: ${companyData.taxNumber}", rightPos, y, paint)
            y += 15f
        }
        if (companyData.billerIban.isNotBlank()) {
            canvas.drawText("IBAN: ${companyData.billerIban}", rightPos, y, paint)
            y += 15f
        }
        if (companyData.billerBIC.isNotBlank()) {
            canvas.drawText("BIC: ${companyData.billerBIC}", rightPos, y, paint)
            y += 15f
        }
    
        // Datum im Header nur wenn KEINE Unterschrift gezeigt wird
        if (!showSignature) {
            y += 15f
            canvas.drawText("Rechnungsdatum: $today", rightPos, y, paint)
        }

        // Titel
        y = 280f
        paint.textAlign = Paint.Align.LEFT
        paintBold.textAlign = Paint.Align.LEFT
        canvas.drawText("Honorarabrechnung", margin, y, titlePaint)
        y += 30f

        canvas.drawText("Rechnung: $formattedInvoiceNumber", margin, y, paintBold)
        y += 30f

        // Empfänger
        if (companyData.customerSecondNameOrOrga.isNotBlank()) {
            canvas.drawText(companyData.customerSecondNameOrOrga, margin, y, paintBold)
            y += 15f
        }
        if (companyData.customerFirstName.isNotBlank()) {
            canvas.drawText(companyData.customerFirstName, margin, y, paint)
            y += 15f
        }
    
        // Zeige Straße an, falls vorhanden
        if (companyData.customerStreet.isNotBlank()) {
            canvas.drawText("${companyData.customerStreet} ${companyData.customerStreetNumber}", margin, y, paint)
            y += 15f
        }
    
        // Zeige Postfach an, falls vorhanden
        if (companyData.customerMailBox.isNotBlank()) {
            canvas.drawText("Postfach ${companyData.customerMailBox}", margin, y, paint)
            y += 15f
        }
    
        if (companyData.customerPlz.isNotBlank() || companyData.customerCityName.isNotBlank()) {
            canvas.drawText("${companyData.customerPlz} ${companyData.customerCityName}", margin, y, paint)
            y += 30f
        }

        // Bildungszentrum Info
        if (companyData.eduCenter.isNotBlank()) {
            canvas.drawText("Bildungszentrum: ${companyData.eduCenter}", margin, y, paint)
            y += 15f
        }
        if (companyData.locationNr.isNotBlank()) {
            canvas.drawText("Standortnummer: ${companyData.locationNr}", margin, y, paint)
            y += 15f
        }
        if (companyData.schoolType.isNotBlank()) {
            canvas.drawText("Schulart / Maßnahme: ${companyData.schoolType}", margin, y, paint)
            y += 15f
        }
        y += 25f

        // Zeitraum + Summen
        val sortedEntries = invoiceWithEntries.entries.sortedBy {
            try { dateFormatter.parse(it.date) } catch (e: Exception) { Date(0) }
        }
        val firstEntry = sortedEntries.firstOrNull()
        if (firstEntry != null) {
            val calendar = Calendar.getInstance()
            try {
                calendar.time = dateFormatter.parse(firstEntry.date) ?: Date()
            } catch (e: Exception) {
                Log.w(TAG, "Datum nicht lesbar: ${firstEntry.date}", e)
            }
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = dateFormatter.format(calendar.time)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val monthEnd = dateFormatter.format(calendar.time)

            val summaryText = "Für den in der Zeit von $monthStart bis $monthEnd erteilten Fachunterricht und/oder andere Tätigkeiten gemäß meiner Aufstellung anbei, stelle ich wie folgt in Rechnung:"
            y = drawTextWrapped(canvas, summaryText, margin, y, paint, contentWidth)
            y += 20f

            // Prüfen ob alle Sätze gleich sind
            val allRates = invoiceWithEntries.entries.map { it.rate }.distinct()
            if (allRates.size == 1) {
                val rate = allRates.first()
                canvas.drawText("UE Gesamt a 45 Min = ${invoiceWithEntries.totalLessonUnit}  x   ${decimalFormat.format(rate)} € Honorar/UE", margin, y, paintBold)
            } else {
                canvas.drawText("UE Gesamt a 45 Min = ${invoiceWithEntries.totalLessonUnit} (verschiedene Sätze gemäß Aufstellung)", margin, y, paintBold)
            }
            y += 20f
            canvas.drawText("Summe = ${decimalFormat.format(invoiceWithEntries.totalSum)} €", margin, y, paintBold)
            y += 40f
        }

        // Unterschrift (recycle() passiert im finally, damit es auch bei einem
        // Zeichenfehler läuft)
        val signature = signatureBitmap
        if (signature != null) {
            try {
                val dstWidth = 150f
                val dstHeight = 50f
                canvas.drawBitmap(signature, null, android.graphics.RectF(margin, y, margin + dstWidth, y + dstHeight), null)
                y += 60f
                canvas.drawLine(margin, y, margin + 200f, y, linePaint)
                y += 15f
                canvas.drawText("${companyData.billerCityName}, $today", margin, y, paint)
            } catch (e: Exception) {
                Log.e(TAG, "Unterschrift konnte nicht gezeichnet werden", e)
            }
        }

        pdfDocument.finishPage(page)

        // --- Ab Seite 2: Tabelle ---
        var currentEntryIndex = 0
        val rowHeight = 25f
        val tableTop = 80f
        val tableBottom = pageHeight - 60f
        var pageNumber = 2
        val stripePaint = Paint().apply { color = Color.LTGRAY; alpha = 30 }

        while (currentEntryIndex < sortedEntries.size) {
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = tableTop

            // Header
            paintBold.textAlign = Paint.Align.LEFT
            canvas.drawText("Datum", margin, y, paintBold)
            canvas.drawText("UE", margin + 80f, y, paintBold)
            canvas.drawText("Kosten", margin + 160f, y, paintBold)
            canvas.drawText("Unterrichtsfach/Klasse", margin + 240f, y, paintBold)
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += rowHeight

            paint.textAlign = Paint.Align.LEFT
            while (currentEntryIndex < sortedEntries.size && y < tableBottom - 40f) {
                val entry = sortedEntries[currentEntryIndex]
            
                // Umrechnung von Stunden in UE für die Tabelle
                val ueValue = entry.lessonUnits.multiply(BigDecimal("60"))
                    .divide(BigDecimal("45"), 10, RoundingMode.HALF_UP)
                val entryCost = ueValue.multiply(entry.rate).setScale(2, RoundingMode.HALF_UP)

                // Zebra-Streifen
                if (currentEntryIndex % 2 == 1) {
                    canvas.drawRect(margin, y - 15f, pageWidth - margin, y + 10f, stripePaint)
                }

                canvas.drawText(entry.date, margin, y, paint)
                canvas.drawText(decimalFormat.format(ueValue), margin + 80f, y, paint)
                canvas.drawText("${decimalFormat.format(entryCost)} €", margin + 160f, y, paint)
            
                // Text Clipping für Unterrichtsfach/Klasse
                canvas.save()
                canvas.clipRect(margin + 240f, y - 15f, pageWidth - margin, y + 10f)
                canvas.drawText(entry.teachingSubject, margin + 240f, y, paint)
                canvas.restore()

                y += rowHeight
                currentEntryIndex++
            }

            // Summen auf der letzten Seite der Tabelle
            if (currentEntryIndex == sortedEntries.size) {
                y += 10f
                canvas.drawText("UE Gesamt a 45 Min = ${invoiceWithEntries.totalLessonUnit}", margin, y, paintBold)
                y += 20f
                canvas.drawText("Summe = ${decimalFormat.format(invoiceWithEntries.totalSum)} €", margin, y, paintBold)
            }

            pdfDocument.finishPage(page)
            pageNumber++
        }

        val fileName = "Rechnung_$formattedInvoiceNumber.pdf"
        return@withContext savePdf(context, pdfDocument, fileName, onFinished)
    } finally {
        // Muss auch laufen, wenn beim Zeichnen etwas schiefgeht:
        // sonst leckt der native Puffer des PdfDocument.
        signatureBitmap?.recycle()
        pdfDocument.close()
    }
}

private fun drawTextWrapped(canvas: Canvas, text: String, x: Float, yStart: Float, paint: Paint, maxWidth: Float): Float {
    val words = text.split(" ")
    var line = ""
    var y = yStart
    for (word in words) {
        val testLine = if (line.isEmpty()) word else "$line $word"
        val width = paint.measureText(testLine)
        if (width > maxWidth) {
            canvas.drawText(line, x, y, paint)
            line = word
            y += paint.textSize + 4
        } else {
            line = testLine
        }
    }
    if (line.isNotEmpty()) {
        canvas.drawText(line, x, y, paint)
        y += paint.textSize + 4
    }
    return y
}

/**
 * Schreibt das Dokument ueber den MediaStore nach Dokumente/HonorarCraft.
 *
 * Seit minSdk 29 gibt es nur noch diesen Weg: der frueher noetige Zweig fuer
 * Android 7 bis 9 schrieb direkt in den oeffentlichen Ordner und brauchte dafuer
 * WRITE_EXTERNAL_STORAGE. Beides ist entfallen.
 */
private suspend fun savePdf(context: Context, pdfDocument: PdfDocument, fileName: String, onFinished: (File?) -> Unit): Boolean {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/HonorarCraft")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
    if (uri == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Fehler beim Erstellen der Datei im MediaStore", Toast.LENGTH_SHORT).show()
        }
        onFinished(null)
        return false
    }
    return try {
        resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "PDF gespeichert unter Dokumente/HonorarCraft", Toast.LENGTH_LONG).show()
        }
        onFinished(null)
        true
    } catch (e: Exception) {
        Log.e(TAG, "PDF konnte nicht gespeichert werden", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Fehler beim Speichern: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        false
    }
}
