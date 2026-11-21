package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [InvestigationLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun investigationDao(): InvestigationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brevard_shadow_atlas.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-seed baseline field investigations
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.investigationDao()?.let { dao ->
                                    dao.insertLog(
                                        InvestigationLog(
                                            locationId = "compound-palmbay",
                                            locationName = "The Compound",
                                            timestamp = System.currentTimeMillis() - 86400000L * 2,
                                            investigatorCallsign = "Shadow-01 [Vance]",
                                            notes = "Sweep conducted at 02:40 AM near 47th St grid intersection. Compass needle deviated 42 degrees west. Vehicle radio picked up periodic choral harmonizing on 98.4 MHz. Sudden temperature plunge from 78°F to 59°F within 30 yards.",
                                            emfReadingPeak = 86.4f,
                                            anomalyIntensity = 5,
                                            evpDetected = true,
                                            audioNotesSummary = "Static burst containing phrase 'turn back'",
                                            moonPhase = "Full Moon"
                                        )
                                    )
                                    dao.insertLog(
                                        InvestigationLog(
                                            locationId = "ashleys-rockledge",
                                            locationName = "Ashley's Restaurant & Tavern",
                                            timestamp = System.currentTimeMillis() - 86400000L * 5,
                                            investigatorCallsign = "Spectre-04 [Chen]",
                                            notes = "Investigated second-floor dining area post-closing. Restroom mirror experienced sudden condensation forming letters 'E.A.'. Pint glass slid 8 inches across polished mahogany bar counter with no slope.",
                                            emfReadingPeak = 74.2f,
                                            anomalyIntensity = 4,
                                            evpDetected = true,
                                            audioNotesSummary = "EVP: Female voice whisper 'Jack'",
                                            moonPhase = "Waxing Gibbous"
                                        )
                                    )
                                    dao.insertLog(
                                        InvestigationLog(
                                            locationId = "lc34-cape-canaveral",
                                            locationName = "Launch Complex 34",
                                            timestamp = System.currentTimeMillis() - 86400000L * 12,
                                            investigatorCallsign = "Apollo-Tracker [Reyes]",
                                            notes = "Permit access at 01:15 AM near the concrete launch pedestal. Strong persistent ozone scent without electrical equipment running. Handheld VHF scanner locked on anomalous transmission pulse.",
                                            emfReadingPeak = 92.1f,
                                            anomalyIntensity = 4,
                                            evpDetected = true,
                                            audioNotesSummary = "Telemetry crackle and countdown fragment",
                                            moonPhase = "New Moon"
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
