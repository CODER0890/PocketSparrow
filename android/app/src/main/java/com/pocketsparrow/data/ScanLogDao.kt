package com.pocketsparrow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ScanLogEntity)

    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ScanLogEntity>>

    @Query("SELECT * FROM scan_logs WHERE threatLevel = 2 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentThreats(limit: Int): Flow<List<ScanLogEntity>>

    @Query("SELECT COUNT(*) FROM scan_logs WHERE threatLevel = 2")
    fun getThreatCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_logs")
    fun getTotalCount(): Flow<Int>
}
