package de.v404.honorarcraftandroid

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explizite Migrationspfade für die Room-Datenbank.
 *
 * Bis Version 9 lief die App mit `fallbackToDestructiveMigration`, d. h. jede Erhöhung
 * von [AppDatabase.version] hat die komplette Datenbank des Nutzers verworfen. Das ist
 * für eine Abrechnungs-App nicht tragbar; ab hier wird jeder Schritt migriert.
 *
 * Die Pfade sind aus den exportierten Schemas unter `app/schemas/` abgeleitet.
 */

/**
 * Schritt ohne Strukturänderung.
 *
 * Die Schemas 3/4 sowie 6/7/8/9 tragen jeweils denselben `identityHash`
 * (`e6f1985c…` bzw. `65b085cd…`), die Versionsnummer wurde also ohne echte
 * Schemaänderung erhöht. Room verlangt trotzdem einen registrierten Pfad.
 */
private fun noOp(from: Int, to: Int) = object : Migration(from, to) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

val MIGRATION_3_4 = noOp(3, 4)
val MIGRATION_6_7 = noOp(6, 7)
val MIGRATION_7_8 = noOp(7, 8)
val MIGRATION_8_9 = noOp(8, 9)

/**
 * 4 → 5: Der Honorarsatz wandert von der Rechnung auf die einzelne Position,
 * damit eine Rechnung gemischte Sätze enthalten kann. Bestandspositionen erben
 * den Satz ihrer Rechnung.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `invoice_entries` ADD COLUMN `rate` TEXT NOT NULL DEFAULT '23.00'"
        )
        db.execSQL(
            """
            UPDATE `invoice_entries`
               SET `rate` = (
                   SELECT i.`rate` FROM `invoices` i
                    WHERE i.`invoiceNumber` = `invoice_entries`.`invoiceNumber`
               )
             WHERE EXISTS (
                   SELECT 1 FROM `invoices` i
                    WHERE i.`invoiceNumber` = `invoice_entries`.`invoiceNumber`
               )
            """.trimIndent()
        )
    }
}

/**
 * 5 → 6: `invoices.rate` entfällt, weil der Satz jetzt an der Position hängt.
 *
 * SQLite vor 3.35 (Android < 12) kennt kein `DROP COLUMN`, die Elterntabelle muss
 * also neu gebaut werden.
 *
 * Achtung: `DROP TABLE invoices` löst das `ON DELETE CASCADE` von
 * `invoice_entries` aus und würde alle Positionen mitlöschen. `defer_foreign_keys`
 * verschiebt nur die *Prüfung* der Constraints, nicht die Cascade-Aktion selbst –
 * es reicht hier nicht. Ob Room die Migration mit aktivierten Foreign Keys
 * ausführt, ist ein Implementierungsdetail, auf das man sich nicht verlassen
 * sollte; deshalb werden die Positionen vor dem Umbau in eine Hilfstabelle ohne
 * FK-Beziehung kopiert und danach zurückgespielt. Gegen SQLite verifiziert für
 * `foreign_keys = ON` und `= OFF`.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA defer_foreign_keys = TRUE")

        // Positionen sichern (CREATE TABLE AS SELECT übernimmt keine Constraints)
        db.execSQL("CREATE TABLE `_entries_backup` AS SELECT * FROM `invoice_entries`")

        // Elterntabelle ohne die Spalte `rate` neu aufbauen
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `_new_invoices` " +
                "(`invoiceNumber` TEXT NOT NULL, PRIMARY KEY(`invoiceNumber`))"
        )
        db.execSQL(
            "INSERT INTO `_new_invoices` (`invoiceNumber`) " +
                "SELECT `invoiceNumber` FROM `invoices`"
        )
        db.execSQL("DROP TABLE `invoices`")
        db.execSQL("ALTER TABLE `_new_invoices` RENAME TO `invoices`")

        // Positionen zurückspielen (Indizes von `invoice_entries` bleiben erhalten,
        // die Tabelle selbst wurde nie gelöscht)
        db.execSQL("DELETE FROM `invoice_entries`")
        db.execSQL(
            "INSERT INTO `invoice_entries` " +
                "(`id`, `invoiceNumber`, `date`, `lessonUnits`, `teachingSubject`, `rate`) " +
                "SELECT `id`, `invoiceNumber`, `date`, `lessonUnits`, `teachingSubject`, `rate` " +
                "FROM `_entries_backup`"
        )
        db.execSQL("DROP TABLE `_entries_backup`")
    }
}

/**
 * 9 → 10: Ausgeblendete Fachvorschläge bekommen eine eigene Tabelle.
 *
 * Vorher löschte das "X" am Vorschlag alle Positionen mit diesem Fach aus allen
 * Rechnungen. Jetzt wird nur noch der Vorschlag ausgeblendet; die Belege bleiben
 * unangetastet. Die Tabelle startet leer – es gibt nichts zu übernehmen.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `hidden_subjects` " +
                "(`name` TEXT NOT NULL, PRIMARY KEY(`name`))"
        )
    }
}

/** Alle Migrationen in einer Liste – auch für den Migrationstest verwendbar. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
)
