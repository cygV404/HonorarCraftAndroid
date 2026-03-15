package com.juliandobrodolac.honorarcraftandroid

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

    @Query("""
        SELECT SUM((e.lessonUnits * 60.0 / 45.0) * i.rate) 
        FROM invoice_entries e 
        JOIN invoices i ON e.invoiceNumber = i.invoiceNumber 
        WHERE e.date LIKE '%' || :year
    """)
    fun getRevenueForYear(year: String): Flow<Double?>
}

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyData(companyData: CompanyData)

    @Query("SELECT * FROM company_data WHERE id = 1")
    fun getCompanyData(): Flow<CompanyData?>
}

@Database(entities = [InvoiceData::class, InvoiceEntry::class, CompanyData::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
