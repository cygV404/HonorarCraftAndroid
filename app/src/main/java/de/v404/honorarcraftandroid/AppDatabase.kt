package de.v404.honorarcraftandroid

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: InvoiceEntry)

    @Delete
    suspend fun deleteEntry(entry: InvoiceEntry)

    @Delete
    suspend fun deleteEntries(entries: List<InvoiceEntry>)

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun getInvoice(invoiceNumber: String): InvoiceData?

    @Transaction
    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber")
    fun getInvoiceWithEntries(invoiceNumber: String): Flow<InvoiceWithEntries?>

    @Query("SELECT teachingSubject FROM invoice_entries WHERE teachingSubject != '' GROUP BY teachingSubject ORDER BY COUNT(*) DESC")
    fun getUniqueSubjects(): Flow<List<String>>

    @Query("DELETE FROM invoice_entries WHERE teachingSubject = :subject")
    suspend fun deleteEntriesBySubject(subject: String)

    @Query("SELECT invoiceNumber FROM invoices")
    fun getAllInvoiceNumbers(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM invoices")
    fun getAllInvoicesWithEntries(): Flow<List<InvoiceWithEntries>>

    @Transaction
    @Query("""
        SELECT DISTINCT i.* FROM invoices i 
        JOIN invoice_entries e ON i.invoiceNumber = e.invoiceNumber 
        WHERE e.date LIKE '%' || :year
    """)
    fun getInvoicesWithEntriesByYear(year: String): Flow<List<InvoiceWithEntries>>
}

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyData(companyData: CompanyData)

    @Query("SELECT * FROM company_data WHERE id = 1")
    fun getCompanyData(): Flow<CompanyData?>
}

@Database(
    entities = [InvoiceData::class, InvoiceEntry::class, CompanyData::class],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun companyDao(): CompanyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "honorarcraft_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
