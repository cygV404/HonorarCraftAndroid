package de.v404.honorarcraftandroid

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "Backup"

/**
 * Der Import ist gescheitert, nachdem die Datenbankverbindung bereits geschlossen
 * war. Die Daten sind zurueckgerollt, die App muss aber trotzdem neu starten -
 * sonst laeuft sie auf einer geschlossenen Verbindung weiter.
 */
class NeustartNoetigException(message: String, cause: Throwable?) : Exception(message, cause)

/**
 * Export und Import der kompletten Datenbank in eine vom Nutzer gewählte Datei.
 *
 * Warum das nötig ist: die App hat `allowBackup="false"`, damit Steuernummer,
 * IBAN und Umsätze nicht im Google-Backup landen. Ohne einen eigenen Weg gäbe es
 * damit gar keine Möglichkeit mehr, Daten auf ein neues Gerät zu bekommen oder
 * sich gegen einen Verlust abzusichern.
 *
 * Format ist die Room-Datenbankdatei selbst. Das klingt roh, hat aber einen
 * handfesten Vorteil: eine ältere Sicherung wird beim Öffnen automatisch über die
 * Migrationskette in [Migrations] auf den aktuellen Stand gehoben.
 */
object Backup {

    const val DATABASE_NAME = "honorarcraft_database"
    const val MIME_TYPE = "application/octet-stream"

    /** Dateiname mit Datum, damit mehrere Sicherungen nebeneinander liegen können. */
    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
        return "HonorarCraft_Sicherung_$stamp.hcbackup"
    }

    /**
     * Schreibt die Datenbank in [target].
     *
     * Der entscheidende Schritt ist der WAL-Checkpoint: Room läuft im
     * Write-Ahead-Logging-Modus, frische Änderungen stehen also noch in der
     * `-wal`-Datei und nicht in der Hauptdatei. Ohne `TRUNCATE`-Checkpoint würde
     * die Sicherung genau die zuletzt erfassten Positionen nicht enthalten.
     */
    suspend fun export(context: Context, target: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

            val quelle = context.getDatabasePath(DATABASE_NAME)
            check(quelle.exists()) { "Datenbankdatei nicht gefunden" }

            context.contentResolver.openOutputStream(target).use { out ->
                checkNotNull(out) { "Zieldatei konnte nicht geöffnet werden" }
                quelle.inputStream().use { input -> input.copyTo(out) }
            }
            quelle.length()
        }.onFailure { Log.e(TAG, "Export fehlgeschlagen", it) }
    }

    /**
     * Ersetzt die Datenbank durch den Inhalt von [source].
     *
     * Bewusst vorsichtig: die Datei wird erst in den Cache kopiert und geprüft,
     * und die bestehende Datenbank wird gesichert, bevor irgendetwas überschrieben
     * wird. Schlägt der Austausch mittendrin fehl, wird der alte Stand
     * zurückgerollt – ein misslungener Import darf nicht die Daten kosten, die
     * vorher da waren.
     *
     * Der Aufrufer muss die App danach neu starten: die bestehenden Room-Flows
     * hängen an der alten, inzwischen geschlossenen Verbindung.
     */
    suspend fun import(context: Context, source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val kandidat = File(context.cacheDir, "import_candidate.db")
        val sicherung = File(context.cacheDir, "pre_import_backup.db")
        runCatching {
            // 1. Datei in den Cache holen
            context.contentResolver.openInputStream(source).use { input ->
                checkNotNull(input) { "Datei konnte nicht geöffnet werden" }
                kandidat.outputStream().use { out -> input.copyTo(out) }
            }

            // 2. Prüfen, bevor irgendetwas angefasst wird
            val version = pruefe(kandidat)

            // 3. Aktuellen Stand sichern und Datenbank schliessen
            val ziel = context.getDatabasePath(DATABASE_NAME)
            AppDatabase.closeInstance()
            if (ziel.exists()) ziel.copyTo(sicherung, overwrite = true)

            try {
                // 4. Austauschen. -wal und -shm muessen weg, sonst mischt SQLite
                //    das alte Write-Ahead-Log in die neue Datei.
                File(ziel.parentFile, "$DATABASE_NAME-wal").delete()
                File(ziel.parentFile, "$DATABASE_NAME-shm").delete()
                kandidat.copyTo(ziel, overwrite = true)
            } catch (e: Throwable) {
                Log.e(TAG, "Austausch fehlgeschlagen, rolle zurück", e)
                if (sicherung.exists()) sicherung.copyTo(ziel, overwrite = true)
                // Die Daten sind gerettet, aber die Verbindung ist zu diesem
                // Zeitpunkt schon geschlossen - ohne Neustart wuerde die App nur
                // noch leere Listen zeigen. Deshalb als eigener Fehlertyp, damit
                // der Aufrufer trotzdem neu startet.
                throw NeustartNoetigException(
                    "Einlesen fehlgeschlagen. Der vorherige Stand wurde " +
                        "wiederhergestellt, die App startet neu.",
                    e
                )
            }
            version
        }.onFailure {
            Log.e(TAG, "Import fehlgeschlagen", it)
        }.also {
            kandidat.delete()
            sicherung.delete()
        }
    }

    /**
     * Stellt sicher, dass [datei] wirklich eine HonorarCraft-Datenbank ist, und
     * liefert ihre Schemaversion.
     *
     * Ohne diese Prüfung würde eine beliebige ausgewählte Datei die Buchhaltung
     * überschreiben und die App beim nächsten Start nur noch abstürzen.
     */
    private fun pruefe(datei: File): Int {
        val db = try {
            SQLiteDatabase.openDatabase(datei.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            throw IllegalArgumentException("Das ist keine gültige Sicherungsdatei.", e)
        }
        db.use {
            val tabellen = mutableSetOf<String>()
            it.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                while (c.moveToNext()) tabellen.add(c.getString(0))
            }
            val fehlend = listOf("invoices", "invoice_entries", "company_data") - tabellen
            require(fehlend.isEmpty()) {
                "Die Datei stammt nicht von HonorarCraft (fehlende Tabellen: ${fehlend.joinToString()})."
            }

            val version = it.version
            require(version in 3..DATABASE_VERSION) {
                if (version > DATABASE_VERSION) {
                    "Die Sicherung stammt aus einer neueren App-Version. Bitte zuerst die App aktualisieren."
                } else {
                    "Die Sicherung ist zu alt und kann nicht mehr gelesen werden (Version $version)."
                }
            }
            return version
        }
    }
}
