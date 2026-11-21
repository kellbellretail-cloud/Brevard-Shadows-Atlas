package com.example.ui.voicebox

enum class VoiceBoxBand(
    val label: String,
    val minMhz: Float,
    val maxMhz: Float,
    val stepDelayMs: Long
) {
    FM_SWEEP("FM 87.5-108", 87.5f, 108.0f, 150L),
    VHF_BAND("VHF 118-136", 118.0f, 136.0f, 185L),
    EMF_LEX("EMF-LEX", 88.0f, 108.0f, 220L)
}

enum class VoiceBoxSpeaker {
    SPIRIT,
    INVESTIGATOR,
    SYSTEM
}

data class VoiceBoxEntry(
    val id: String,
    val speaker: VoiceBoxSpeaker,
    val text: String,
    val frequencyMhz: Float? = null,
    val emfMicroTesla: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class VoiceBoxUiState(
    val isActive: Boolean = false,
    val isWriting: Boolean = false,
    val currentlyWritingText: String = "",
    val currentlyWritingFreqMhz: Float = 96.9f,
    val currentlyWritingEmfUt: Float = 44.2f,
    val lastWrittenText: String = "",
    val currentFrequencyMhz: Float = 96.9f,
    val selectedBand: VoiceBoxBand = VoiceBoxBand.FM_SWEEP,
    val audioStaticEnabled: Boolean = true,
    val spokenVoiceEnabled: Boolean = true,
    val hasHardwareEmfSensor: Boolean = false,
    val liveEmfUt: Float = 44.2f,
    val micEnergyLevel: Float = 0f,
    val micListening: Boolean = false,
    val statusLine: String = "STANDBY • READY TO SWEEP"
)
