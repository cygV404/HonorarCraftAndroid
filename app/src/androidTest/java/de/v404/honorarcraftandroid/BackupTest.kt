package de.v404.honorarcraftandroid

import android.content.Context
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal

/**
 * Prüft Sicherung und Wiederherstellung der Datenbank.
 *
 * Das ist das Sicherheitsnetz für alle Daten der App: mit `allowBackup="false"`
 * gibt es keinen anderen Weg mehr, Rechnungen auf ein neues Gerät zu bekommen.
 * Ein Fehler hier fällt erst auf, wenn jemand die Sicherung wirklich braucht –
 * also genau dann, wenn es zu spät ist.
 *
 * Ausführen: `./gradlew connectedAndroidTest` (löscht die App danach vom Gerät,
 * deshalb nur auf einem Emulator laufen lassen).
 */
@RunWith(AndroidJUnit4::class)
class BackupTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var sicherungsDatei: File

    @Before
    fun aufraeumenUndVorbereiten() {
        AppDatabase.closeInstance()
        context.getDatabasePath(Backup.DATABASE_NAME).also { db ->
            db.delete()
            File(db.parentFile, "${Backup.DATABASE_NAME}-wal").delete()
            File(db.parentFile, "${Backup.DATABASE_NAME}-shm").delete()
        }
        sicherungsDatei = File(context.cacheDir, "test_backup.hcbackup").apply { delete() }
    }

    @After
    fun schliessen() {
        AppDatabase.closeInstance()
        sicherungsDatei.delete()
    }

    private fun eintragAnlegen(rechnung: String, fach: String, stunden: String) = runBlocking {
        val dao = AppDatabase.getDatabase(context).invoiceDao()
        if (dao.getInvoice(rechnung) == null) dao.insertInvoice(InvoiceData(rechnung))
        dao.insertEntry(
            InvoiceEntry(
                invoiceNumber = rechnung,
                date = "02.09.2026",
                lessonUnits = BigDecimal(stunden),
                teachingSubject = fach,
                rate = BigDecimal("23.00")
            )
        )
    }

    private fun faecher(): List<String> {
        val db = AppDatabase.getDatabase(context).openHelper.readableDatabase
        return db.query("SELECT teachingSubject FROM invoice_entries").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
    }

    private fun anzahlPositionen(): Int {
        val db = AppDatabase.getDatabase(context).openHelper.readableDatabase
        return db.query("SELECT COUNT(*) FROM invoice_entries").use { c ->
            c.moveToFirst(); c.getInt(0)
        }
    }

    /**
     * Der wichtigste Test: schreiben, Daten zerstören, zurückholen.
     *
     * Enthält bewusst einen Eintrag, der unmittelbar vor dem Export angelegt wird
     * und deshalb noch im Write-Ahead-Log steht – ohne den WAL-Checkpoint in
     * [Backup.export] würde genau der in der Sicherung fehlen.
     */
    @Test
    fun rundlauf_sicherungEnthaeltAuchFrischeEintraege() = runBlocking<Unit> {
        eintragAnlegen("01", "Mathe", "3")
        eintragAnlegen("01", "Deutsch", "2")
        eintragAnlegen("02", "FrischVorDemExport", "1")
        assertEquals(3, anzahlPositionen())

        val exportErgebnis = Backup.export(context, sicherungsDatei.toUri())
        assertTrue("Export muss gelingen: ${exportErgebnis.exceptionOrNull()}", exportErgebnis.isSuccess)
        assertTrue("Sicherungsdatei darf nicht leer sein", sicherungsDatei.length() > 0)

        // Datenverlust simulieren
        runBlocking {
            AppDatabase.getDatabase(context).invoiceDao()
                .deleteInvoice(InvoiceData("01"))
        }
        assertEquals("Vorbedingung: Positionen wurden geloescht", 1, anzahlPositionen())

        val importErgebnis = Backup.import(context, sicherungsDatei.toUri())
        assertTrue("Import muss gelingen: ${importErgebnis.exceptionOrNull()}", importErgebnis.isSuccess)

        // Nach dem Import baut getDatabase() die Verbindung neu auf
        assertEquals("Alle drei Positionen muessen zurueck sein", 3, anzahlPositionen())
        assertTrue(
            "Der frisch erfasste Eintrag muss enthalten sein",
            faecher().contains("FrischVorDemExport")
        )
    }

    /** Eine beliebige Datei darf die Buchhaltung nicht überschreiben. */
    @Test
    fun import_lehntFremdeDateiAbUndLaesstDatenUnangetastet() = runBlocking<Unit> {
        eintragAnlegen("01", "Mathe", "3")
        assertEquals(1, anzahlPositionen())

        val muell = File(context.cacheDir, "kein_backup.txt").apply {
            writeText("Das ist nur Text, keine Datenbank.")
        }

        val ergebnis = Backup.import(context, muell.toUri())

        assertTrue("Import muss fehlschlagen", ergebnis.isFailure)
        assertEquals("Bestehende Daten muessen unangetastet bleiben", 1, anzahlPositionen())
        muell.delete()
    }

    /** Eine gültige SQLite-Datei ohne unsere Tabellen ist ebenfalls kein Backup. */
    @Test
    fun import_lehntFremdeDatenbankAb() = runBlocking<Unit> {
        eintragAnlegen("01", "Mathe", "3")

        val fremd = File(context.cacheDir, "fremd.db").apply { delete() }
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(fremd, null).use {
            it.execSQL("CREATE TABLE etwas_anderes(id INTEGER)")
        }

        val ergebnis = Backup.import(context, fremd.toUri())

        assertTrue("Fremde Datenbank muss abgelehnt werden", ergebnis.isFailure)
        assertEquals("Bestehende Daten muessen unangetastet bleiben", 1, anzahlPositionen())
        fremd.delete()
    }
}
