package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.InvestigationLog
import com.example.data.model.ShadowLocation
import com.example.data.model.ShadowLocationsRepository
import com.example.ui.voicebox.VoiceBoxBand
import com.example.ui.voicebox.VoiceBoxEntry
import com.example.ui.voicebox.VoiceBoxResponseEngine
import com.example.ui.voicebox.VoiceBoxSpeaker
import com.example.ui.voicebox.VoiceBoxUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import java.util.UUID
import kotlin.math.sqrt

private const val TAG = "MainViewModel"

enum class AppTab(val title: String) {
    ATLAS_MAP("Radar Map"),
    DIRECTORY("Dossiers"),
    VOICE_BOX("Voice Box"),
    JOURNAL("Logbook")
}

private data class QueuedSpiritTransmission(
    val text: String,
    val frequencyMhz: Float,
    val emfUt: Float
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.investigationDao()

    val locations: List<ShadowLocation> = ShadowLocationsRepository.locations

    private val _currentTab = MutableStateFlow(AppTab.ATLAS_MAP)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedLocationForMap = MutableStateFlow<ShadowLocation?>(null)
    val selectedLocationForMap: StateFlow<ShadowLocation?> = _selectedLocationForMap.asStateFlow()

    private val _activeDetailLocation = MutableStateFlow<ShadowLocation?>(null)
    val activeDetailLocation: StateFlow<ShadowLocation?> = _activeDetailLocation.asStateFlow()

    val investigationLogs: StateFlow<List<InvestigationLog>> = dao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Spirit Voice Box state
    private val _voiceBoxUiState = MutableStateFlow(VoiceBoxUiState())
    val voiceBoxUiState: StateFlow<VoiceBoxUiState> = _voiceBoxUiState.asStateFlow()

    private val _voiceBoxContextLocation = MutableStateFlow<ShadowLocation?>(null)
    val voiceBoxContextLocation: StateFlow<ShadowLocation?> = _voiceBoxContextLocation.asStateFlow()

    private val _voiceBoxEntries = MutableStateFlow<List<VoiceBoxEntry>>(emptyList())
    val voiceBoxEntries: StateFlow<List<VoiceBoxEntry>> = _voiceBoxEntries.asStateFlow()

    private val transmissionQueue = Channel<QueuedSpiritTransmission>(Channel.UNLIMITED)
    private var writerLoopJob: Job? = null
    private var sweepJob: Job? = null
    private var staticAudioJob: Job? = null
    private var micMonitorJob: Job? = null

    private var sensorManager: SensorManager? = null
    private var magneticListener: SensorEventListener? = null
    private var lastBaselineEmf = 44.2f
    private var lastSpikeTriggerMs = 0L

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val vibrator: Vibrator? by lazy {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        startWriterLoop()
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setPitch(0.74f)
                    tts?.setSpeechRate(0.86f)
                    ttsReady = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech unavailable", e)
        }
    }

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectLocationOnMap(location: ShadowLocation?) {
        _selectedLocationForMap.value = location
    }

    fun openDetailedPage(location: ShadowLocation) {
        _activeDetailLocation.value = location
    }

    fun closeDetailedPage() {
        _activeDetailLocation.value = null
    }

    fun locateAndFocusOnMap(location: ShadowLocation) {
        _selectedLocationForMap.value = location
        _currentTab.value = AppTab.ATLAS_MAP
    }

    fun openVoiceBoxForLocation(location: ShadowLocation?) {
        setVoiceBoxFocusLocation(location)
        _activeDetailLocation.value = null
        _currentTab.value = AppTab.VOICE_BOX
    }

    fun setVoiceBoxFocusLocation(location: ShadowLocation?) {
        _voiceBoxContextLocation.value = location
        location?.let { loc ->
            _voiceBoxUiState.update { it.copy(currentFrequencyMhz = loc.evpFrequencyMhz) }
        }
    }

    fun selectVoiceBoxBand(band: VoiceBoxBand) {
        _voiceBoxUiState.update {
            it.copy(
                selectedBand = band,
                currentFrequencyMhz = (band.minMhz + band.maxMhz) / 2f
            )
        }
    }

    fun toggleVoiceBoxStaticAudio() {
        val next = !_voiceBoxUiState.value.audioStaticEnabled
        _voiceBoxUiState.update { it.copy(audioStaticEnabled = next) }
        if (_voiceBoxUiState.value.isActive) {
            if (next) startStaticAudioLoop() else stopStaticAudioLoop()
        }
    }

    fun toggleVoiceBoxSpokenOutput() {
        val next = !_voiceBoxUiState.value.spokenVoiceEnabled
        _voiceBoxUiState.update { it.copy(spokenVoiceEnabled = next) }
        if (!next) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
    }

    fun clearVoiceBoxEntries() {
        _voiceBoxEntries.value = emptyList()
        _voiceBoxUiState.update {
            it.copy(
                lastWrittenText = "",
                currentlyWritingText = ""
            )
        }
    }

    fun saveVoiceBoxTapeToLogbook() {
        val spiritLines = _voiceBoxEntries.value
            .filter { it.speaker == VoiceBoxSpeaker.SPIRIT }
            .map { "[${String.format("%.1f", it.frequencyMhz ?: 96.9f)} MHz] ${it.text}" }
        if (spiritLines.isEmpty()) return

        val focusLoc = _voiceBoxContextLocation.value ?: locations.first()
        val peakEmf = _voiceBoxEntries.value.mapNotNull { it.emfMicroTesla }.maxOrNull()
            ?: _voiceBoxUiState.value.liveEmfUt

        val log = InvestigationLog(
            locationId = focusLoc.id,
            locationName = focusLoc.name,
            timestamp = System.currentTimeMillis(),
            investigatorCallsign = "Voice Box Session",
            notes = "Spirit Voice Box written captures:\n" + spiritLines.takeLast(8).joinToString("\n"),
            emfReadingPeak = peakEmf,
            anomalyIntensity = if (peakEmf > 75f) 5 else if (peakEmf > 58f) 4 else 3,
            evpDetected = true,
            audioNotesSummary = "${spiritLines.size} physical voice box transcripts logged",
            moonPhase = "Waning Gibbous"
        )
        saveInvestigationLog(log)
        appendVoiceBoxSystemEntry("Saved ${spiritLines.size} spirit transmissions to Logbook (${focusLoc.name}).")
    }

    fun saveInvestigationLog(log: InvestigationLog) {
        viewModelScope.launch {
            dao.insertLog(log)
        }
    }

    fun deleteInvestigationLog(log: InvestigationLog) {
        viewModelScope.launch {
            dao.deleteLog(log)
        }
    }

    fun startVoiceBox(hasMicPermission: Boolean) {
        if (_voiceBoxUiState.value.isActive) return

        val focusName = _voiceBoxContextLocation.value?.name ?: "Brevard County Open Band"
        _voiceBoxUiState.update {
            it.copy(
                isActive = true,
                statusLine = if (hasMicPermission) {
                    "SWEEPING LIVE • MIC + EMF ARMED"
                } else {
                    "SWEEPING LIVE • EMF SENSOR ARMED"
                }
            )
        }
        appendVoiceBoxSystemEntry("Voice Box powered on ($focusName). Listening for breakthroughs...")

        registerMagnetometerSensor()
        startFrequencySweepLoop()
        if (_voiceBoxUiState.value.audioStaticEnabled) {
            startStaticAudioLoop()
        }
        if (hasMicPermission) {
            startMicrophoneListener()
        }
    }

    fun stopVoiceBox() {
        if (!_voiceBoxUiState.value.isActive) return
        sweepJob?.cancel()
        sweepJob = null
        stopStaticAudioLoop()
        micMonitorJob?.cancel()
        micMonitorJob = null
        unregisterMagnetometerSensor()

        try {
            tts?.stop()
        } catch (_: Exception) {}

        _voiceBoxUiState.update {
            it.copy(
                isActive = false,
                micListening = false,
                micEnergyLevel = 0f,
                statusLine = "STANDBY • READY TO SWEEP"
            )
        }
        appendVoiceBoxSystemEntry("Voice Box powered off.")
    }

    fun askSpiritsQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        _voiceBoxEntries.update { current ->
            current + VoiceBoxEntry(
                id = UUID.randomUUID().toString(),
                speaker = VoiceBoxSpeaker.INVESTIGATOR,
                text = trimmed
            )
        }

        val state = _voiceBoxUiState.value
        val targetLoc = _voiceBoxContextLocation.value
        val reply = VoiceBoxResponseEngine.generateReplyForQuestion(
            question = trimmed,
            location = targetLoc,
            emfUt = state.liveEmfUt,
            random = Random()
        )
        val lockFreq = targetLoc?.evpFrequencyMhz ?: state.currentFrequencyMhz
        enqueueSpiritTransmission(
            text = reply,
            frequencyMhz = lockFreq,
            emfUt = state.liveEmfUt
        )
    }

    private fun enqueueSpiritTransmission(
        text: String,
        frequencyMhz: Float,
        emfUt: Float
    ) {
        transmissionQueue.trySend(
            QueuedSpiritTransmission(
                text = text,
                frequencyMhz = frequencyMhz,
                emfUt = emfUt
            )
        )
    }

    private fun startWriterLoop() {
        writerLoopJob?.cancel()
        writerLoopJob = viewModelScope.launch {
            for (item in transmissionQueue) {
                if (!isActive) break

                _voiceBoxUiState.update {
                    it.copy(
                        isWriting = true,
                        currentlyWritingText = "",
                        currentlyWritingFreqMhz = item.frequencyMhz,
                        currentlyWritingEmfUt = item.emfUt,
                        statusLine = "SPIRIT BREAKTHROUGH • WRITING..."
                    )
                }

                // Trigger brief haptic pulse when a transmission locks onto the display
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } catch (_: Exception) {}

                // Speak via built-in Android TTS if enabled
                if (_voiceBoxUiState.value.spokenVoiceEnabled && ttsReady) {
                    try {
                        tts?.speak(item.text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
                    } catch (_: Exception) {}
                }

                // Physically write out each character one by one onto the Voice Box display
                val builder = StringBuilder()
                for (ch in item.text) {
                    if (!isActive) break
                    builder.append(ch)
                    _voiceBoxUiState.update {
                        it.copy(currentlyWritingText = builder.toString())
                    }
                    val charDelay = when (ch) {
                        '.', ',', '!', '?' -> 115L
                        ' ' -> 45L
                        else -> 58L
                    }
                    delay(charDelay)
                }

                // Commit finished message to the printed Voice Box tape
                _voiceBoxEntries.update { current ->
                    current + VoiceBoxEntry(
                        id = UUID.randomUUID().toString(),
                        speaker = VoiceBoxSpeaker.SPIRIT,
                        text = item.text,
                        frequencyMhz = item.frequencyMhz,
                        emfMicroTesla = item.emfUt
                    )
                }

                _voiceBoxUiState.update { state ->
                    state.copy(
                        isWriting = false,
                        currentlyWritingText = "",
                        lastWrittenText = item.text,
                        statusLine = if (state.isActive) {
                            if (state.micListening) "SWEEPING LIVE • MIC + EMF ARMED" else "SWEEPING LIVE • EMF SENSOR ARMED"
                        } else {
                            "STANDBY • READY TO SWEEP"
                        }
                    )
                }

                delay(350L)
            }
        }
    }

    private fun startFrequencySweepLoop() {
        sweepJob?.cancel()
        sweepJob = viewModelScope.launch {
            val random = Random()
            var ticksSinceLastTransmission = 0
            while (isActive && _voiceBoxUiState.value.isActive) {
                val state = _voiceBoxUiState.value
                val band = state.selectedBand
                delay(band.stepDelayMs)

                val nextFreq = band.minMhz + random.nextFloat() * (band.maxMhz - band.minMhz)
                _voiceBoxUiState.update { it.copy(currentFrequencyMhz = nextFreq) }

                ticksSinceLastTransmission++
                // Periodic ambient breakthrough (~every 5–7s if not already writing)
                if (!state.isWriting && ticksSinceLastTransmission >= 28 && random.nextInt(10) == 0) {
                    ticksSinceLastTransmission = 0
                    val msg = VoiceBoxResponseEngine.generateAmbientTransmission(
                        location = _voiceBoxContextLocation.value,
                        emfUt = state.liveEmfUt,
                        random = random
                    )
                    enqueueSpiritTransmission(
                        text = msg,
                        frequencyMhz = nextFreq,
                        emfUt = state.liveEmfUt
                    )
                }
            }
        }
    }

    private fun startStaticAudioLoop() {
        staticAudioJob?.cancel()
        staticAudioJob = viewModelScope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuffer <= 0) return@launch

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuffer)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                val random = Random()
                val pcm = ShortArray(minBuffer / 2)
                while (isActive && _voiceBoxUiState.value.isActive && _voiceBoxUiState.value.audioStaticEnabled) {
                    val writing = _voiceBoxUiState.value.isWriting
                    val amp = if (writing) 700 else 1900
                    for (i in pcm.indices) {
                        pcm[i] = (random.nextInt(amp * 2) - amp).toShort()
                    }
                    track.write(pcm, 0, pcm.size)
                    delay(40L)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Static audio track unavailable", e)
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopStaticAudioLoop() {
        staticAudioJob?.cancel()
        staticAudioJob = null
    }

    private fun startMicrophoneListener() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        micMonitorJob?.cancel()
        micMonitorJob = viewModelScope.launch(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val sampleRate = 16000
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (bufferSize <= 0) return@launch

                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    return@launch
                }

                recorder.startRecording()
                _voiceBoxUiState.update { it.copy(micListening = true) }

                val buffer = ShortArray(bufferSize / 2)
                val random = Random()
                var speechFrames = 0
                var lastVoiceResponseMs = 0L

                while (isActive && _voiceBoxUiState.value.isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val s = buffer[i].toDouble()
                            sumSq += s * s
                        }
                        val rms = sqrt(sumSq / read)
                        val normalized = (rms / 6500.0).toFloat().coerceIn(0f, 1f)
                        _voiceBoxUiState.update { it.copy(micEnergyLevel = normalized) }

                        if (normalized > 0.22f) {
                            speechFrames++
                        } else if (speechFrames >= 4) {
                            // User spoke a phrase/knock and paused -> trigger a spirit write-out!
                            speechFrames = 0
                            val now = System.currentTimeMillis()
                            if (now - lastVoiceResponseMs > 4500L && !_voiceBoxUiState.value.isWriting) {
                                lastVoiceResponseMs = now
                                val cur = _voiceBoxUiState.value
                                val msg = VoiceBoxResponseEngine.generateAmbientTransmission(
                                    location = _voiceBoxContextLocation.value,
                                    emfUt = cur.liveEmfUt,
                                    random = random
                                )
                                enqueueSpiritTransmission(
                                    text = msg,
                                    frequencyMhz = cur.currentFrequencyMhz,
                                    emfUt = cur.liveEmfUt
                                )
                            }
                        } else {
                            speechFrames = 0
                        }
                    }
                    delay(80L)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.w(TAG, "Microphone permission unavailable", e)
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord monitor unavailable", e)
            } finally {
                _voiceBoxUiState.update { it.copy(micListening = false, micEnergyLevel = 0f) }
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun registerMagnetometerSensor() {
        val ctx = getApplication<Application>()
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        _voiceBoxUiState.update { it.copy(hasHardwareEmfSensor = magSensor != null) }
        if (magSensor == null) return

        sensorManager = sm
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_MAGNETIC_FIELD) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(mag - lastBaselineEmf)
                lastBaselineEmf = lastBaselineEmf * 0.9f + mag * 0.1f

                _voiceBoxUiState.update { it.copy(liveEmfUt = mag) }

                val now = System.currentTimeMillis()
                if (delta > 6.5f && now - lastSpikeTriggerMs > 6000L && !_voiceBoxUiState.value.isWriting) {
                    lastSpikeTriggerMs = now
                    val cur = _voiceBoxUiState.value
                    val msg = VoiceBoxResponseEngine.generateAmbientTransmission(
                        location = _voiceBoxContextLocation.value,
                        emfUt = mag,
                        random = Random()
                    )
                    enqueueSpiritTransmission(
                        text = msg,
                        frequencyMhz = cur.currentFrequencyMhz,
                        emfUt = mag
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        magneticListener = listener
        sm.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterMagnetometerSensor() {
        magneticListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
        magneticListener = null
    }

    private fun appendVoiceBoxSystemEntry(text: String) {
        _voiceBoxEntries.update { current ->
            current + VoiceBoxEntry(
                id = UUID.randomUUID().toString(),
                speaker = VoiceBoxSpeaker.SYSTEM,
                text = text
            )
        }
    }

    override fun onCleared() {
        sweepJob?.cancel()
        stopStaticAudioLoop()
        micMonitorJob?.cancel()
        writerLoopJob?.cancel()
        unregisterMagnetometerSensor()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        super.onCleared()
    }
}
