package com.pocketsparrow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SpamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumber(number: SpamNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumbers(numbers: List<SpamNumberEntity>)

    @Query("SELECT * FROM spam_numbers WHERE hash = :hash LIMIT 1")
    suspend fun getNumberByHash(hash: String): SpamNumberEntity?

    @Query("SELECT * FROM spam_numbers ORDER BY lastSeen DESC")
    suspend fun getAllNumbers(): List<SpamNumberEntity>

    @Query("SELECT COUNT(*) FROM spam_numbers")
    suspend fun getNumberCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSender(sender: SpamSenderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenders(senders: List<SpamSenderEntity>)

    @Query("SELECT * FROM spam_senders WHERE hash = :hash LIMIT 1")
    suspend fun getSenderByHash(hash: String): SpamSenderEntity?

    @Query("SELECT * FROM spam_senders ORDER BY lastSeen DESC")
    suspend fun getAllSenders(): List<SpamSenderEntity>

    @Query("SELECT COUNT(*) FROM spam_senders")
    suspend fun getSenderCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: SpamPatternEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatterns(patterns: List<SpamPatternEntity>)

    @Query("SELECT * FROM spam_patterns ORDER BY hits DESC")
    suspend fun getAllPatterns(): List<SpamPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: UserReportEntity)

    @Query("SELECT * FROM user_reports ORDER BY timestamp DESC")
    suspend fun getAllReports(): List<UserReportEntity>

    @Query("SELECT COUNT(*) FROM user_reports")
    suspend fun getReportCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarantined(msg: QuarantinedMessageEntity)

    @Query("SELECT * FROM quarantined_messages ORDER BY timestamp DESC")
    suspend fun getAllQuarantined(): List<QuarantinedMessageEntity>

    @Query("SELECT COUNT(*) FROM quarantined_messages")
    suspend fun getQuarantinedCount(): Int
}
