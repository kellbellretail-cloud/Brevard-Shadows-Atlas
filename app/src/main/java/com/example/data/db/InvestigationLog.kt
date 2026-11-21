package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investigation_logs")
data class InvestigationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationId: String,
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val investigatorCallsign: String,
    val notes: String,
    val emfReadingPeak: Float,
    val anomalyIntensity: Int, // 1 to 5
    val evpDetected: Boolean,
    val audioNotesSummary: String? = null,
    val moonPhase: String = "Waning Crescent"
)
