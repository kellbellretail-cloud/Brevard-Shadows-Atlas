package com.example.ui.detail

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import kotlin.math.sin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.InvestigationLog
import com.example.data.model.AnomalyClass
import com.example.data.model.ShadowLocation
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GhostAmber
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.PhosphorCyan
import com.example.ui.theme.SpectralEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningCrimson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    location: ShadowLocation,
    onBack: () -> Unit,
    onSaveInvestigation: (InvestigationLog) -> Unit,
    onOpenVoiceBox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scrollState = rememberScrollState()
    var isPlayingEvp by remember { mutableStateOf(false) }
    var tunedFrequency by remember { mutableFloatStateOf(location.evpFrequencyMhz) }
    var showLogDialog by remember { mutableStateOf(false) }

    // Audio synthesizer for spectral static and eerie bursts
    val coroutineScope = rememberCoroutineScope()
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }

    val classColor = when (location.anomalyClass) {
        AnomalyClass.CLASS_V_APPARITION -> WarningCrimson
        AnomalyClass.CLASS_IV_POLTERGEIST -> GhostAmber
        AnomalyClass.CLASS_III_ACTIVE -> SpectralEmerald
        AnomalyClass.CLASS_II_ATMOSPHERIC, AnomalyClass.CLASS_I_SUBTLE -> PhosphorCyan
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("location_detail_screen"),
        containerColor = MidnightObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DOSSIER // ${location.id.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = PhosphorCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to map",
                            tint = PhosphorCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenVoiceBox,
                        modifier = Modifier.testTag("detail_open_voice_box_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Open Spirit Voice Box for ${location.name}",
                            tint = SpectralEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface.copy(alpha = 0.95f),
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = PhosphorCyan,
                contentColor = Color(0xFF002229),
                modifier = Modifier.testTag("fab_log_investigation")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Log Field Investigation"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Investigation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Hero Image Header with gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Image(
                    painter = painterResource(id = location.drawableRes),
                    contentDescription = location.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top & Bottom shadow gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MidnightObsidian.copy(alpha = 0.6f),
                                    MidnightObsidian
                                ),
                                startY = 60f
                            )
                        )
                )

                // Floating classification badges
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = classColor.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, classColor)
                        ) {
                            Text(
                                text = location.anomalyClass.label.uppercase(),
                                color = classColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = location.category.title.uppercase(),
                                color = PhosphorCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "${location.municipality}, ${location.county}, FL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (location.county != "Brevard County") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GhostAmber.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GhostAmber.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "ROAD TRIP • ${location.travelNote}",
                                color = GhostAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Quick Stats HUD Strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PhosphorCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COORDINATES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = String.format("%.3f°N, %.3f°W", location.latitude, kotlin.math.abs(location.longitude)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkSurfaceVariant))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = GhostAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PRIME HOURS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = location.bestInvestigationHours,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GhostAmber
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkSurfaceVariant))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = SpectralEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WITNESSES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "${location.witnessCount}+ Filed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SpectralEmerald
                        )
                    }
                }
            }

            // Historical Lore Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTORICAL RECORD & URBAN LORE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PhosphorCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = location.fullLore,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Field Evidence & Anomaly Notes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PARANORMAL INVESTIGATION EVIDENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SpectralEmerald,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = location.evidenceNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GhostAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACCESS & FIELD SAFETY GUIDELINES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GhostAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${location.accessStatus} • Threat: ${location.threatLevel}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Interactive EVP Frequency Scanner / Spirit Receiver
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = PhosphorCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SPECTRAL FREQUENCY RECEIVER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = PhosphorCyan
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPlayingEvp) SpectralEmerald.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlayingEvp) SpectralEmerald else DarkSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (isPlayingEvp) "ACTIVE SWEEP" else "STANDBY",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPlayingEvp) SpectralEmerald else TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Frequency Band: ${String.format("%.1f", tunedFrequency)} MHz (Target: ${location.evpFrequencyMhz} MHz)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Slider(
                        value = tunedFrequency,
                        onValueChange = { tunedFrequency = it },
                        valueRange = 88.0f..108.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = PhosphorCyan,
                            activeTrackColor = PhosphorCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    // Audio sweep playback button
                    Button(
                        onClick = {
                            if (isPlayingEvp) {
                                isPlayingEvp = false
                                try {
                                    audioTrack?.stop()
                                    audioTrack?.release()
                                    audioTrack = null
                                } catch (_: Exception) {}
                            } else {
                                isPlayingEvp = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val sampleRate = 22050
                                    val bufferSize = AudioTrack.getMinBufferSize(
                                        sampleRate,
                                        AudioFormat.CHANNEL_OUT_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT
                                    )
                                    val track = AudioTrack.Builder()
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
                                        .setBufferSizeInBytes(bufferSize)
                                        .setTransferMode(AudioTrack.MODE_STREAM)
                                        .build()

                                    audioTrack = track
                                    track.play()

                                    val random = Random()
                                    val pcm = ShortArray(bufferSize / 2)
                                    var elapsed = 0
                                    while (isActive && isPlayingEvp && elapsed < 8000) {
                                        for (i in pcm.indices) {
                                            // Synthesize atmospheric radio static + faint tone oscillation
                                            val whiteNoise = (random.nextInt(4000) - 2000).toShort()
                                            val tone = (sin(i * 0.08) * 1200).toInt().toShort()
                                            pcm[i] = (whiteNoise / 2 + tone / 2).toShort()
                                        }
                                        track.write(pcm, 0, pcm.size)
                                        elapsed += 100
                                        delay(30)
                                    }
                                    track.stop()
                                    track.release()
                                    audioTrack = null
                                    isPlayingEvp = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingEvp) WarningCrimson else PhosphorCyan,
                            contentColor = if (isPlayingEvp) Color.White else Color(0xFF002229)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("toggle_evp_sweep")
                    ) {
                        Icon(
                            imageVector = if (isPlayingEvp) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlayingEvp) "Stop Spectral Receiver" else "Tune Spectral Receiver (${location.evpFrequencyMhz} MHz)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Decoded transcript box
                    AnimatedVisibility(visible = isPlayingEvp || tunedFrequency in (location.evpFrequencyMhz - 0.4f)..(location.evpFrequencyMhz + 0.4f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightObsidian)
                                .border(1.dp, SpectralEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "CAPTURED EVP TRANSCRIPT // FREQUENCY LOCKED",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SpectralEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = location.evpTranscript,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp)) // Extra space for FAB
        }
    }

    // Modal Dialog to log a new field investigation for this location
    if (showLogDialog) {
        NewInvestigationDialog(
            location = location,
            onDismiss = { showLogDialog = false },
            onSave = { log ->
                onSaveInvestigation(log)
                showLogDialog = false
            }
        )
    }
}

@Composable
fun NewInvestigationDialog(
    location: ShadowLocation,
    onDismiss: () -> Unit,
    onSave: (InvestigationLog) -> Unit
) {
    var callsign by remember { mutableStateOf("Investigator") }
    var notes by remember { mutableStateOf("") }
    var emfPeak by remember { mutableFloatStateOf(65.0f) }
    var intensity by remember { mutableIntStateOf(3) }
    var evpDetected by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(18.dp))
                .border(1.dp, PhosphorCyan, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FIELD INVESTIGATION LOG",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PhosphorCyan,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it },
                    label = { Text("Investigator Callsign / Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_callsign")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Field Notes & Sightings") },
                    placeholder = { Text("E.g., Compass deviated, strange cold draft near perimeter...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("input_notes")
                )

                Column {
                    Text(
                        text = "Peak EMF Reading: ${String.format("%.1f", emfPeak)} µT",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GhostAmber
                    )
                    Slider(
                        value = emfPeak,
                        onValueChange = { emfPeak = it },
                        valueRange = 25f..150f,
                        colors = SliderDefaults.colors(
                            thumbColor = GhostAmber,
                            activeTrackColor = GhostAmber
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anomaly Severity (1-5):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..5) {
                            Surface(
                                shape = CircleShape,
                                color = if (intensity == i) WarningCrimson else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (intensity == i) WarningCrimson else DarkSurfaceVariant),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { intensity = i }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$i",
                                        color = if (intensity == i) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val newLog = InvestigationLog(
                                locationId = location.id,
                                locationName = location.name,
                                timestamp = System.currentTimeMillis(),
                                investigatorCallsign = callsign.ifBlank { "Agent" },
                                notes = notes.ifBlank { "Routine paranormal patrol completed. Minor static anomalies logged." },
                                emfReadingPeak = emfPeak,
                                anomalyIntensity = intensity,
                                evpDetected = evpDetected,
                                moonPhase = "Full Moon"
                            )
                            onSave(newLog)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PhosphorCyan,
                            contentColor = Color(0xFF002229)
                        ),
                        modifier = Modifier.testTag("submit_investigation_log")
                    ) {
                        Text("Record Field Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
