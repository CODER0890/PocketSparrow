package com.pocketsparrow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ScanLogEntity::class,
        SpamNumberEntity::class,
        SpamSenderEntity::class,
        SpamPatternEntity::class,
        UserReportEntity::class,
        QuarantinedMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanLogDao(): ScanLogDao
    abstract fun spamDao(): SpamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // In production, master key is retrieved from Android Keystore System
        private val PASSPHRASE = "pocket_sparrow_aes256_airgap_master_key".toByteArray()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(PASSPHRASE)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_sparrow_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
