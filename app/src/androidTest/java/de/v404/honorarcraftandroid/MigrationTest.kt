package de.v404.honorarcraftandroid

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die Migrationskette von [AppDatabase].
 *
 * Hintergrund: Bis Version 9 lief die App mit `fallbackToDestructiveMigration`,
 * d. h. jedes Schema-Update hat die Rechnungen des Nutzers gelöscht. Diese Tests
 * sichern ab, dass das nicht wieder passiert.
 *
 * Ausführen: `./gradlew connectedAndroidTest` (benötigt Gerät oder Emulator).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Der kritische Schritt: 4 → 5 verschiebt den Honorarsatz von der Rechnung auf
     * die Position, 5 → 6 baut dafür die Elterntabelle um. Dabei darf das
     * `ON DELETE CASCADE` von `invoice_entries` nicht auslösen.
     */
    @Test
    fun migrate4To10_behaeltPositionenUndUebernimmtHonorarsaetze() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL("INSERT INTO invoices (invoiceNumber, rate) VALUES ('01', '23.00')")
            db.execSQL("INSERT INTO invoices (invoiceNumber, rate) VALUES ('02', '31.50')")
            db.execSQL(
                "INSERT INTO invoice_entries (invoiceNumber, date, lessonUnits, teachingSubject) " +
                    "VALUES ('01', '04.03.2026', '2.0', 'Mathe')"
            )
            db.execSQL(
                "INSERT INTO invoice_entries (invoiceNumber, date, lessonUnits, teachingSubject) " +
                    "VALUES ('01', '05.03.2026', '1.5', 'Deutsch')"
            )
            db.execSQL(
                "INSERT INTO invoice_entries (invoiceNumber, date, lessonUnits, teachingSubject) " +
                    "VALUES ('02', '11.04.2026', '3.0', 'Englisch')"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, *ALL_MIGRATIONS)

        db.query("SELECT COUNT(*) FROM invoice_entries").use { c ->
            c.moveToFirst()
            assertEquals("Keine Position darf verloren gehen", 3, c.getInt(0))
        }
        db.query(
            "SELECT rate FROM invoice_entries WHERE teachingSubject = 'Englisch'"
        ).use { c ->
            c.moveToFirst()
            assertEquals(
                "Position muss den Satz ihrer Rechnung geerbt haben",
                "31.50",
                c.getString(0)
            )
        }
        db.query("SELECT COUNT(*) FROM invoices").use { c ->
            c.moveToFirst()
            assertEquals(2, c.getInt(0))
        }
    }

    /** 6 → 9 sind reine Versionssprünge ohne Schemaänderung (gleicher identityHash). */
    @Test
    fun migrate6To10_laeuftDurch() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL("INSERT INTO invoices (invoiceNumber) VALUES ('07')")
            db.execSQL(
                "INSERT INTO invoice_entries (invoiceNumber, date, lessonUnits, teachingSubject, rate) " +
                    "VALUES ('07', '01.09.2026', '4.0', 'Physik', '28.00')"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, *ALL_MIGRATIONS)

        db.query("SELECT teachingSubject FROM invoice_entries").use { c ->
            c.moveToFirst()
            assertEquals("Physik", c.getString(0))
        }
    }

    /** Ab 10 gibt es die Tabelle für ausgeblendete Fachvorschläge. */
    @Test
    fun migrate9To10_legtHiddenSubjectsAn() {
        helper.createDatabase(TEST_DB, 9).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, *ALL_MIGRATIONS)

        db.execSQL("INSERT INTO hidden_subjects (name) VALUES ('Mathe')")
        db.query("SELECT COUNT(*) FROM hidden_subjects").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }
}
