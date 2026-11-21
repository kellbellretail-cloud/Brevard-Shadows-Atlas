package com.example.ui.voicebox

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun VoiceBoxScreen(
    uiState: VoiceBoxUiState,
    contextLocation: ShadowLocation?,
    availableLocations: List<ShadowLocation>,
    entries: List<VoiceBoxEntry>,
    onSelectBand: (VoiceBoxBand) -> Unit,
    onSelectFocusLocation: (ShadowLocation?) -> Unit,
    onToggleStaticAudio: () -> Unit,
    onToggleSpokenVoice: () -> Unit,
    onStartVoiceBox: (hasMicPermission: Boolean) -> Unit,
    onStopVoiceBox: () -> Unit,
    onAskSpiritsQuestion: (String) -> Unit,
    onSaveTapeToLogbook: () -> Unit,
    onClearEntries: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showSaveConfirmed by remember { mutableStateOf(false) }
    var questionInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        onStartVoiceBox(granted)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            uiState.isWriting -> GhostAmber
            uiState.isActive -> SpectralEmerald
            else -> TextSecondary
        },
        label = "voiceBoxStatusColor"
    )

    // Blinking cursor for the physical spirit write-out box
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceBoxBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightObsidian)
            .testTag("voice_box_screen")
    ) {
        val isCompact = maxHeight < 580.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))

            // Top Header & Audio/Save Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPIRIT VOICE BOX // EVP WRITER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PhosphorCyan,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = if (isCompact && contextLocation != null) {
                            "${contextLocation.name} • ${uiState.statusLine}"
                        } else {
                            uiState.statusLine
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleStaticAudio,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_voicebox_static")
                    ) {
                        Icon(
                            imageVector = if (uiState.audioStaticEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle radio static sweep audio",
                            tint = if (uiState.audioStaticEnabled) PhosphorCyan else TextMuted
                        )
                    }

                    IconButton(
                        onClick = onToggleSpokenVoice,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_voicebox_tts")
                    ) {
                        Icon(
                            imageVector = if (uiState.spokenVoiceEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                            contentDescription = "Toggle spoken spirit voice output",
                            tint = if (uiState.spokenVoiceEnabled) SpectralEmerald else TextMuted
                        )
                    }

                    if (entries.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSaveTapeToLogbook()
                                showSaveConfirmed = true
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("save_voicebox_tape_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save spirit tape to logbook",
                                tint = GhostAmber
                            )
                        }

                        IconButton(
                            onClick = {
                                showSaveConfirmed = false
                                onClearEntries()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_voicebox_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear voice box tape",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSaveConfirmed) {
                Text(
                    text = "SAVED TRANSCRIPT TO FIELD LOGBOOK",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SpectralEmerald,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Sector Focus Pill / Quick Site Tuner (dedicated bar when not compact)
            if (!isCompact) {
                if (contextLocation != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PhosphorCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tuned Sector: ${contextLocation.name} (${contextLocation.evpFrequencyMhz} MHz)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "CHANGE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GhostAmber,
                                modifier = Modifier
                                    .clickable { onSelectFocusLocation(null) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableLocations.take(8).forEach { loc ->
                            FilterChip(
                                selected = false,
                                onClick = { onSelectFocusLocation(loc) },
                                label = {
                                    Text(
                                        text = loc.name,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Band Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceBoxBand.entries.forEach { band ->
                    val selected = band == uiState.selectedBand
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectBand(band) },
                        label = {
                            Text(
                                text = band.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PhosphorCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PhosphorCyan,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) PhosphorCyan else DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("band_chip_${band.name.lowercase()}")
                    )
                }
            }

            if (!isCompact) {
                Spacer(modifier = Modifier.height(6.dp))

                // Hardware Frequency, Live EMF & Acoustic Oscilloscope Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "${String.format("%.1f", uiState.currentFrequencyMhz)} MHz",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (uiState.isActive) SpectralEmerald else TextPrimary
                            )
                            Text(
                                text = "EMF ${String.format("%.1f", uiState.liveEmfUt)} µT • ${if (uiState.micListening) "MIC LIVE" else "SWEEP"}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (uiState.liveEmfUt > 65f) GhostAmber else TextSecondary
                            )
                        }

                        // Real-time Oscilloscope Canvas reacting to mic energy & spirit transmissions
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MidnightObsidian)
                                .padding(horizontal = 4.dp)
                        ) {
                            val midY = size.height / 2f
                            val amplitude = when {
                                uiState.isWriting -> size.height * 0.40f
                                uiState.isActive -> size.height * (0.12f + uiState.micEnergyLevel.coerceIn(0f, 1f) * 0.34f)
                                else -> size.height * 0.04f
                            }
                            val path = Path()
                            val steps = 36
                            for (i in 0..steps) {
                                val x = (i / steps.toFloat()) * size.width
                                val norm = i / steps.toFloat()
                                val wave = sin(wavePhase * 2f + norm * 14f) *
                                    sin(norm * PI.toFloat()) * amplitude
                                val y = midY + wave
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = statusColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            // HERO COMPONENT: Physical Spirit Typewriter Display Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spirit_writer_display"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF040D10),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (uiState.isWriting) GhostAmber else if (uiState.isActive) SpectralEmerald.copy(alpha = 0.7f) else DarkSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isWriting) Icons.Default.GraphicEq else Icons.Default.Radio,
                                contentDescription = null,
                                tint = if (uiState.isWriting) GhostAmber else PhosphorCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isWriting) "SPIRIT WRITING LIVE..." else "VOICE BOX DISPLAY",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isWriting) GhostAmber else PhosphorCyan,
                                letterSpacing = 0.8.sp
                            )
                        }

                        if (uiState.isWriting || uiState.lastWrittenText.isNotEmpty()) {
                            Text(
                                text = "${String.format("%.1f", uiState.currentlyWritingFreqMhz)} MHz • ${String.format("%.1f", uiState.currentlyWritingEmfUt)} µT",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

                    val displayText = when {
                        uiState.isWriting -> uiState.currentlyWritingText
                        uiState.lastWrittenText.isNotEmpty() -> uiState.lastWrittenText
                        uiState.isActive -> "Scanning open bands... Waiting for spirit response"
                        else -> "Power on the Spirit Voice Box to capture & write out spirit speech"
                    }
                    val cursor = if (uiState.isWriting || uiState.isActive) " █" else ""

                    Text(
                        text = displayText + cursor,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (uiState.isWriting || uiState.lastWrittenText.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            uiState.isWriting -> GhostAmber
                            uiState.lastWrittenText.isNotEmpty() -> SpectralEmerald.copy(alpha = 0.95f)
                            else -> TextSecondary.copy(alpha = 0.85f * cursorAlpha + 0.15f)
                        },
                        lineHeight = 20.sp,
                        minLines = if (isCompact) 1 else 3,
                        maxLines = if (isCompact) 3 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            // Printed Spirit Tape (Written Transcript Log)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Printed Voice Box Tape is empty.\nActivate the sweep or ask a question below—every spirit word will physically write itself across the display and log here.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isCompact) 6.dp else 10.dp)
                            .testTag("voicebox_tape_list"),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            VoiceBoxTapeItem(entry = entry)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 6.dp))

            // Quick Spirit Interrogation Questions (shown when vertical room allows)
            if (!isCompact) {
                val quickQuestions = remember {
                    listOf(
                        "Who is here?",
                        "What happened here?",
                        "Are you friendly?",
                        "Give us a sign"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickQuestions.forEach { q ->
                        SuggestionChip(
                            onClick = { onAskSpiritsQuestion(q) },
                            label = {
                                Text(
                                    text = q,
                                    fontSize = 11.sp,
                                    color = PhosphorCyan
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurfaceElevated
                            ),
                            border = BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    placeholder = { Text("Ask the spirits a question...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (questionInput.isNotBlank()) {
                                onAskSpiritsQuestion(questionInput.trim())
                                questionInput = ""
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = PhosphorCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voicebox_question_input")
                )

                IconButton(
                    onClick = {
                        if (questionInput.isNotBlank()) {
                            onAskSpiritsQuestion(questionInput.trim())
                            questionInput = ""
                        }
                    },
                    enabled = questionInput.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PhosphorCyan,
                        contentColor = MidnightObsidian,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voicebox_send_question_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Ask question on Voice Box"
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 6.dp))

            // Primary Power On / Off Voice Box Button
            Button(
                onClick = {
                    if (uiState.isActive) {
                        onStopVoiceBox()
                    } else if (hasAudioPermission) {
                        onStartVoiceBox(true)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isActive) WarningCrimson else PhosphorCyan,
                    contentColor = if (uiState.isActive) Color.White else MidnightObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("voicebox_primary_button")
            ) {
                Icon(
                    imageVector = when {
                        uiState.isActive -> Icons.Default.PowerSettingsNew
                        hasAudioPermission -> Icons.Default.Mic
                        else -> Icons.Default.MicOff
                    },
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        uiState.isActive -> "Power Off Spirit Voice Box"
                        hasAudioPermission -> "Activate Spirit Voice Box"
                        else -> "Enable Mic & Activate Voice Box"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
        }
    }
}

@Composable
private fun VoiceBoxTapeItem(entry: VoiceBoxEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val timeStr = remember(entry.timestamp) { timeFormat.format(Date(entry.timestamp)) }

    when (entry.speaker) {
        VoiceBoxSpeaker.SYSTEM -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "[$timeStr] ${entry.text}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
        VoiceBoxSpeaker.INVESTIGATOR -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 300.dp),
                    shape = RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp),
                    color = PhosphorCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "INVESTIGATOR • $timeStr",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PhosphorCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.text,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        VoiceBoxSpeaker.SPIRIT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 310.dp),
                    shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp),
                    color = Color(0xFF0A1614),
                    border = BorderStroke(1.dp, GhostAmber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        val meta = buildString {
                            append("SPIRIT EVP • $timeStr")
                            entry.frequencyMhz?.let { append(" • ${String.format("%.1f", it)} MHz") }
                            entry.emfMicroTesla?.let { append(" • ${String.format("%.1f", it)} µT") }
                        }
                        Text(
                            text = meta,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GhostAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.text,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SpectralEmerald,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
