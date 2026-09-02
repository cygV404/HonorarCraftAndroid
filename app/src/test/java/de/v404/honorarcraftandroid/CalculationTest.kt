package de.v404.honorarcraftandroid

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Tests für die Rechenlogik der Abrechnung.
 *
 * Hier entscheidet sich, ob auf der Rechnung der richtige Betrag steht – ein
 * Fehler wirkt sich unmittelbar auf einen Beleg aus, den der Nutzer weitergibt.
 * Alles hier ist reines Kotlin ohne Android-Abhängigkeit und läuft deshalb ohne
 * Emulator: `./gradlew test`
 *
 * Fachlicher Hintergrund: gebucht werden Zeitstunden, abgerechnet wird in
 * Unterrichtseinheiten à 45 Minuten. Eine Zeitstunde sind also 60/45 = 4/3 UE.
 */
class CalculationTest {

    private fun entry(stunden: String, satz: String = "23.00", datum: String = "02.09.2026") =
        InvoiceEntry(
            invoiceNumber = "01",
            date = datum,
            lessonUnits = BigDecimal(stunden),
            teachingSubject = "Mathe",
            rate = BigDecimal(satz)
        )

    private fun invoice(vararg entries: InvoiceEntry) =
        InvoiceWithEntries(InvoiceData("01"), entries.toList())

    // ---------- UE-Umrechnung und Summen ----------

    @Test
    fun `drei Zeitstunden ergeben vier Unterrichtseinheiten`() {
        assertEquals(BigDecimal("4.00"), invoice(entry("3")).totalLessonUnit)
    }

    @Test
    fun `Summe einer Position ist UE mal Satz`() {
        // 3 h -> 4 UE, 4 * 23,00 = 92,00
        assertEquals(BigDecimal("92.00"), invoice(entry("3")).totalSum)
    }

    @Test
    fun `Drittelstunden runden kaufmaennisch auf zwei Stellen`() {
        // 1 h -> 1,333… UE; 1,333… * 23,00 = 30,666… -> 30,67
        val rechnung = invoice(entry("1"))
        assertEquals(BigDecimal("1.33"), rechnung.totalLessonUnit)
        assertEquals(BigDecimal("30.67"), rechnung.totalSum)
    }

    @Test
    fun `Rundung passiert erst auf der Gesamtsumme, nicht je Position`() {
        // Drei mal 1 h: einzeln gerundet waere 3 * 30,67 = 92,01.
        // Korrekt ist erst summieren, dann runden: 4 UE * 23,00 = 92,00
        val rechnung = invoice(entry("1"), entry("1"), entry("1"))
        assertEquals(BigDecimal("92.00"), rechnung.totalSum)
    }

    @Test
    fun `gemischte Honorarsaetze werden je Position gerechnet`() {
        // 3 h zu 23,00 = 92,00  +  3 h zu 31,50 = 126,00
        val rechnung = invoice(entry("3", satz = "23.00"), entry("3", satz = "31.50"))
        assertEquals(BigDecimal("218.00"), rechnung.totalSum)
        assertEquals(BigDecimal("8.00"), rechnung.totalLessonUnit)
    }

    @Test
    fun `leere Rechnung ergibt null Euro statt Absturz`() {
        val leer = invoice()
        assertEquals(BigDecimal("0.00"), leer.totalSum)
        assertEquals(BigDecimal("0.00"), leer.totalLessonUnit)
    }

    @Test
    fun `halbe Stunden rechnen glatt`() {
        // 0,75 h -> 1 UE
        assertEquals(BigDecimal("1.00"), invoice(entry("0.75")).totalLessonUnit)
        assertEquals(BigDecimal("23.00"), invoice(entry("0.75")).totalSum)
    }

    @Test
    fun `realer Fall vom Geraetetest`() {
        // 55 h zu 23,00 -> 73,333… UE -> 1.686,67 EUR
        val rechnung = invoice(entry("55"))
        assertEquals(BigDecimal("73.33"), rechnung.totalLessonUnit)
        assertEquals(BigDecimal("1686.67"), rechnung.totalSum)
    }

    // ---------- Rechnungsnummern-Formatierung ----------

    @Test
    fun `Format NUMBER fuellt auf zwei Stellen auf`() {
        assertEquals("01", formatInvoice("1", InvoiceFormat.NUMBER, 2026, 9))
        assertEquals("07", formatInvoice("7", InvoiceFormat.NUMBER, 2026, 9))
        assertEquals("13", formatInvoice("13", InvoiceFormat.NUMBER, 2026, 9))
    }

    @Test
    fun `Format YEAR_NUMBER stellt das Jahr voran`() {
        assertEquals("2026-01", formatInvoice("1", InvoiceFormat.YEAR_NUMBER, 2026, 9))
    }

    @Test
    fun `Format YEAR_MONTH_NUMBER fuellt Monat und Nummer auf`() {
        assertEquals("2026-09-01", formatInvoice("1", InvoiceFormat.YEAR_MONTH_NUMBER, 2026, 9))
        assertEquals("2026-12-13", formatInvoice("13", InvoiceFormat.YEAR_MONTH_NUMBER, 2026, 12))
    }

    @Test
    fun `dreistellige Nummern werden nicht abgeschnitten`() {
        assertEquals("100", formatInvoice("100", InvoiceFormat.NUMBER, 2026, 9))
        assertEquals("2026-100", formatInvoice("100", InvoiceFormat.YEAR_NUMBER, 2026, 9))
    }

    @Test
    fun `nicht numerische Nummer bleibt unveraendert stehen`() {
        // Nummern koennen aus SharedPreferences kommen; ein unerwarteter Wert darf
        // nicht zu einer Exception fuehren.
        assertEquals("ABC", formatInvoice("ABC", InvoiceFormat.NUMBER, 2026, 9))
    }

    // ---------- Jahreszuordnung ----------

    @Test
    fun `Jahreszuordnung haengt am Ende des Datums`() {
        // Der Jahresumsatz filtert ueber date.endsWith(jahr) - genau deshalb muessen
        // Datumseingaben getrimmt sein, sonst faellt die Position aus der Auswertung.
        assertEquals(true, entry("1", datum = "31.12.2026").date.endsWith("2026"))
        assertEquals(false, entry("1", datum = "01.01.2027").date.endsWith("2026"))
        assertEquals(false, entry("1", datum = "31.12.2026 ").date.endsWith("2026"))
    }
}
