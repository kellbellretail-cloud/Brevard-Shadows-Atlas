package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestigationDao {
    @Query("SELECT * FROM investigation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<InvestigationLog>>

    @Query("SELECT * FROM investigation_logs WHERE locationId = :locId ORDER BY timestamp DESC")
    fun getLogsForLocation(locId: String): Flow<List<InvestigationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InvestigationLog): Long

    @Delete
    suspend fun deleteLog(log: InvestigationLog)

    @Query("SELECT COUNT(*) FROM investigation_logs")
    suspend fun getLogCount(): Int
}
