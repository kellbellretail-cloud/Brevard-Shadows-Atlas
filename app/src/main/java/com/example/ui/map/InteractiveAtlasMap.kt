package com.example.ui.map

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnomalyClass
import com.example.data.model.LocationCategory
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Bounding box strictly covering Brevard County, Florida
private const val MIN_LAT = 27.80
private const val MAX_LAT = 28.80
private const val MIN_LNG = -80.98
private const val MAX_LNG = -80.40

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveAtlasMap(
    locations: List<ShadowLocation>,
    selectedLocation: ShadowLocation?,
    onSelectLocation: (ShadowLocation?) -> Unit,
    onOpenDetails: (ShadowLocation) -> Unit,
    onOpenVoiceBox: (ShadowLocation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var selectedCategory by remember { mutableStateOf(LocationCategory.ALL) }
    var selectedCountyScope by remember { mutableStateOf("All") }
    var showCategoryFilter by remember { mutableStateOf(false) }

    // Real-time EMF Sensor readings from hardware
    var rawSensorEmf by remember { mutableFloatStateOf(44.2f) }
    var hasMagneticSensor by remember { mutableStateOf(false) }
    var hasReceivedSensorEvent by remember { mutableStateOf(false) }
    var isEmfExpanded by remember { mutableStateOf(true) }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Register hardware magnetic field sensor (prefer uncalibrated raw field for spike sensitivity)
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasMagneticSensor = magneticSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 3) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    rawSensorEmf = magnitude
                    hasReceivedSensorEvent = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        magneticSensor?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Pulsing animation for radar & pins
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    val filteredLocations = remember(locations, selectedCategory, selectedCountyScope) {
        locations.filter { loc ->
            val catMatch = selectedCategory == LocationCategory.ALL || loc.category == selectedCategory
            val sectorMatch = when (selectedCountyScope) {
                "All" -> true
                "North" -> loc.latitude >= 28.53
                "Central" -> loc.latitude in 28.20..28.53
                "South" -> loc.latitude < 28.20
                else -> true
            }
            catMatch && sectorMatch
        }
    }

    // Ensure a location focused externally (e.g., from Dossiers) is never hidden by active filters
    LaunchedEffect(selectedLocation) {
        if (selectedLocation != null && filteredLocations.none { it.id == selectedLocation.id }) {
            selectedCategory = LocationCategory.ALL
            selectedCountyScope = "All"
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightObsidian)
            .testTag("interactive_atlas_map")
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()
        val centerX = canvasWidthPx / 2f
        val centerY = canvasHeightPx / 2f

        // Projection helper
        fun projectCoordinate(lat: Double, lng: Double): Offset {
            val normX = (lng - MIN_LNG) / (MAX_LNG - MIN_LNG)
            val normY = 1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT) // Invert Y so North is up

            val baseW = canvasWidthPx * 0.82f
            val baseH = canvasHeightPx * 0.86f
            val startX = (canvasWidthPx - baseW) / 2f
            val startY = (canvasHeightPx - baseH) / 2f

            val rawX = startX + normX.toFloat() * baseW
            val rawY = startY + normY.toFloat() * baseH

            val projectedX = centerX + (rawX - centerX) * zoomScale + panOffsetX
            val projectedY = centerY + (rawY - centerY) * zoomScale + panOffsetY
            return Offset(projectedX, projectedY)
        }

        // Calculate proximity of currently centered/explored map viewpoint to nearest anomaly
        // Reverse project map center (centerX, centerY) back to lat/lng
        val baseW = canvasWidthPx * 0.82f
        val baseH = canvasHeightPx * 0.86f
        val startX = (canvasWidthPx - baseW) / 2f
        val startY = (canvasHeightPx - baseH) / 2f

        val centerMapX = (centerX - panOffsetX - centerX) / zoomScale + centerX
        val centerMapY = (centerY - panOffsetY - centerY) / zoomScale + centerY

        val normCenterLng = ((centerMapX - startX) / baseW).coerceIn(0f, 1f)
        val normCenterLat = (1f - (centerMapY - startY) / baseH).coerceIn(0f, 1f)

        val exploredLat = MIN_LAT + normCenterLat * (MAX_LAT - MIN_LAT)
        val exploredLng = MIN_LNG + normCenterLng * (MAX_LNG - MIN_LNG)

        // Find nearest anomaly to explored viewpoint
        var minDistanceDeg = Double.MAX_VALUE
        var closestLoc: ShadowLocation? = null
        locations.forEach { loc ->
            val dLat = loc.latitude - exploredLat
            val dLng = loc.longitude - exploredLng
            val dist = sqrt(dLat * dLat + dLng * dLng)
            if (dist < minDistanceDeg) {
                minDistanceDeg = dist
                closestLoc = loc
            }
        }

        // Calculate anomalous EMF boost (0 to 45 µT) as user explores close to a hotspot
        val proximityFactor = ((0.28 - minDistanceDeg).coerceAtLeast(0.0) / 0.28).toFloat()
        val anomalySeverityMultiplier = (closestLoc?.anomalyClass?.levelNumber ?: 1) * 0.22f
        val hotspotEmfBoost = proximityFactor * 42f * anomalySeverityMultiplier

        // Combine hardware magnetic sensor readings + local geospatial anomaly field
        val currentTotalEmf = rawSensorEmf + hotspotEmfBoost

        // Smoothly animated EMF value
        val animatedEmf by animateFloatAsState(
            targetValue = currentTotalEmf,
            animationSpec = tween(150),
            label = "animatedTotalEmf"
        )

        // Trigger single subtle tactile pulse when entering an intense anomaly zone
        val inCriticalZone = animatedEmf > 75f && proximityFactor > 0.65f
        LaunchedEffect(inCriticalZone) {
            if (inCriticalZone && vibrator != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } catch (_: Exception) {}
            }
        }

        // Map gesture detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.75f, 4.5f)
                        panOffsetX = (panOffsetX + pan.x).coerceIn(-canvasWidthPx * 1.5f, canvasWidthPx * 1.5f)
                        panOffsetY = (panOffsetY + pan.y).coerceIn(-canvasHeightPx * 1.5f, canvasHeightPx * 1.5f)
                    }
                }
        ) {
            // Tactical Radar & Geography Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw tactical coordinate grid
                val gridLinesX = 6
                for (i in 0..gridLinesX) {
                    val lineX = (w / gridLinesX) * i
                    drawLine(
                        color = Color(0xFF0D1B2A).copy(alpha = 0.5f),
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, h),
                        strokeWidth = 1f
                    )
                }
                val gridLinesY = 8
                for (j in 0..gridLinesY) {
                    val lineY = (h / gridLinesY) * j
                    drawLine(
                        color = Color(0xFF0D1B2A).copy(alpha = 0.5f),
                        start = Offset(0f, lineY),
                        end = Offset(w, lineY),
                        strokeWidth = 1f
                    )
                }

                // Draw Brevard County Indian River Lagoon & Atlantic barrier coastline
                val lagoonPath = Path().apply {
                    val pHaulover = projectCoordinate(28.75, -80.75)
                    val pTitusville = projectCoordinate(28.61, -80.80)
                    val pCocoa = projectCoordinate(28.35, -80.72)
                    val pRockledge = projectCoordinate(28.32, -80.72)
                    val pMelbourne = projectCoordinate(28.08, -80.60)
                    val pPalmBay = projectCoordinate(27.97, -80.57)
                    val pMicco = projectCoordinate(27.88, -80.50)
                    val pSebastian = projectCoordinate(27.84, -80.45)

                    moveTo(pHaulover.x, pHaulover.y)
                    lineTo(pTitusville.x, pTitusville.y)
                    lineTo(pCocoa.x, pCocoa.y)
                    lineTo(pRockledge.x, pRockledge.y)
                    lineTo(pMelbourne.x, pMelbourne.y)
                    lineTo(pPalmBay.x, pPalmBay.y)
                    lineTo(pMicco.x, pMicco.y)
                    lineTo(pSebastian.x, pSebastian.y)
                }

                drawPath(
                    path = lagoonPath,
                    color = Color(0xFF003847).copy(alpha = 0.45f),
                    style = Stroke(width = 8f * zoomScale, cap = StrokeCap.Round)
                )

                // Subtle Western St. Johns Marsh & Banana River reference traces
                val corridorPath = Path().apply {
                    val pStJohnsN = projectCoordinate(28.67, -80.93)
                    val pStJohnsMid = projectCoordinate(28.48, -80.91)
                    val pLakePoinsett = projectCoordinate(28.35, -80.87)
                    val pMarshW = projectCoordinate(28.08, -80.82)
                    val pBananaN = projectCoordinate(28.41, -80.64)
                    val pBananaS = projectCoordinate(28.15, -80.60)

                    moveTo(pStJohnsN.x, pStJohnsN.y)
                    lineTo(pStJohnsMid.x, pStJohnsMid.y)
                    lineTo(pLakePoinsett.x, pLakePoinsett.y)
                    lineTo(pMarshW.x, pMarshW.y)
                    moveTo(pBananaN.x, pBananaN.y)
                    lineTo(pBananaS.x, pBananaS.y)
                }
                drawPath(
                    path = corridorPath,
                    color = PhosphorCyan.copy(alpha = 0.14f),
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                // Cape Canaveral barrier bulge
                val capePath = Path().apply {
                    val pCapeTip = projectCoordinate(28.48, -80.52)
                    val pLC34 = projectCoordinate(28.52, -80.55)
                    val pMerritt = projectCoordinate(28.36, -80.64)
                    val pBeach = projectCoordinate(28.18, -80.58)

                    moveTo(pLC34.x, pLC34.y)
                    quadraticTo(pCapeTip.x, pCapeTip.y, pBeach.x, pBeach.y)
                    lineTo(pMerritt.x, pMerritt.y)
                    close()
                }
                drawPath(
                    path = capePath,
                    color = Color(0xFF08272F).copy(alpha = 0.35f)
                )

                // Tactical radar range rings centered in central Brevard County
                val radarCenter = projectCoordinate(28.30, -80.68)
                val maxRadius = w.coerceAtLeast(h) * 0.65f * zoomScale
                val ringCount = 4
                for (r in 1..ringCount) {
                    val radius = (maxRadius / ringCount) * r
                    drawCircle(
                        color = PhosphorCyan.copy(alpha = 0.08f),
                        radius = radius,
                        center = radarCenter,
                        style = Stroke(width = 1.2f)
                    )
                }

                // Rotating radar sweep line
                val sweepAngle = radarPulse * 2f * PI.toFloat()
                val sweepEnd = Offset(
                    radarCenter.x + cos(sweepAngle) * maxRadius,
                    radarCenter.y + sin(sweepAngle) * maxRadius
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PhosphorCyan.copy(alpha = 0.45f),
                            PhosphorCyan.copy(alpha = 0.05f)
                        ),
                        start = radarCenter,
                        end = sweepEnd
                    ),
                    start = radarCenter,
                    end = sweepEnd,
                    strokeWidth = 2f
                )

                // Render anomalous EMF flux haze around hotspots on map
                locations.forEach { loc ->
                    val pinCenter = projectCoordinate(loc.latitude, loc.longitude)
                    val auraColor = when (loc.anomalyClass) {
                        AnomalyClass.CLASS_V_APPARITION -> WarningCrimson
                        AnomalyClass.CLASS_IV_POLTERGEIST -> GhostAmber
                        AnomalyClass.CLASS_III_ACTIVE -> SpectralEmerald
                        AnomalyClass.CLASS_II_ATMOSPHERIC, AnomalyClass.CLASS_I_SUBTLE -> PhosphorCyan
                    }
                    val haloRadius = (32f * (loc.anomalyClass.levelNumber * 0.85f)) * zoomScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = 0.22f),
                                auraColor.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = pinCenter,
                            radius = haloRadius
                        ),
                        radius = haloRadius,
                        center = pinCenter
                    )
                }
            }

            // Interactive Pin Composables overlaid in map coordinates
            filteredLocations.forEach { loc ->
                val coords = projectCoordinate(loc.latitude, loc.longitude)
                val isSelected = selectedLocation?.id == loc.id

                val pinColor = when (loc.anomalyClass) {
                    AnomalyClass.CLASS_V_APPARITION -> WarningCrimson
                    AnomalyClass.CLASS_IV_POLTERGEIST -> GhostAmber
                    AnomalyClass.CLASS_III_ACTIVE -> SpectralEmerald
                    AnomalyClass.CLASS_II_ATMOSPHERIC, AnomalyClass.CLASS_I_SUBTLE -> PhosphorCyan
                }

                val isRoadTrip = loc.county != "Brevard County"

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (coords.x - 22.dp.toPx()).roundToInt(),
                                (coords.y - 44.dp.toPx()).roundToInt()
                            )
                        }
                        .size(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectLocation(if (isSelected) null else loc)
                        }
                        .testTag("pin_${loc.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing beacon ring around active pins (amber ring border on Central FL road-trip pins)
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 38.dp * beaconPulse else 28.dp)
                            .clip(CircleShape)
                            .background(pinColor.copy(alpha = if (isSelected) 0.35f else 0.14f))
                            .then(
                                if (isRoadTrip && !isSelected) {
                                    Modifier.border(0.8.dp, GhostAmber.copy(alpha = 0.45f), CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )

                    // Pin core badge
                    Surface(
                        modifier = Modifier
                            .size(if (isSelected) 30.dp else 22.dp)
                            .shadow(elevation = if (isSelected) 8.dp else 4.dp, shape = CircleShape),
                        shape = CircleShape,
                        color = if (isSelected) pinColor else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.5.dp,
                            color = pinColor
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (loc.anomalyClass == AnomalyClass.CLASS_V_APPARITION) {
                                    Icons.Default.Warning
                                } else {
                                    Icons.Default.LocationOn
                                },
                                contentDescription = loc.name,
                                tint = if (isSelected) MidnightObsidian else pinColor,
                                modifier = Modifier.size(if (isSelected) 18.dp else 13.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top HUD Controls: Header Bar & Category Filter Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactical Header Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SpectralEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BREVARD SHADOW ATLAS",
                            color = PhosphorCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filteredLocations.size} SITES)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Filter toggle button
                FilledTonalIconButton(
                    onClick = { showCategoryFilter = !showCategoryFilter },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (showCategoryFilter) PhosphorCyan else DarkSurface.copy(alpha = 0.9f),
                        contentColor = if (showCategoryFilter) MidnightObsidian else PhosphorCyan
                    ),
                    modifier = Modifier.size(36.dp).testTag("toggle_category_filter")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter categories",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real-Time Tactical EMF Detector HUD Component for Map Exploration
            RealTimeEmfMapDetector(
                emfValue = animatedEmf,
                hasHardwareSensor = hasMagneticSensor,
                hasSensorEvents = hasReceivedSensorEvent,
                nearestLocationName = closestLoc?.name ?: "Brevard Ambient",
                proximityFactor = proximityFactor,
                isExpanded = isEmfExpanded,
                onToggleExpand = { isEmfExpanded = !isEmfExpanded },
                modifier = Modifier.fillMaxWidth()
            )

            // Expandable Category & Brevard Sector Chips
            AnimatedVisibility(
                visible = showCategoryFilter,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sectorScopes = listOf(
                            "All" to "All Brevard",
                            "North" to "North (Titusville/Mims)",
                            "Central" to "Central (Cocoa/MI/Cape)",
                            "South" to "South (Melbourne/Palm Bay)"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sectorScopes.forEach { (scopeKey, label) ->
                                val isScopeSelected = selectedCountyScope == scopeKey
                                FilterChip(
                                    selected = isScopeSelected,
                                    onClick = { selectedCountyScope = scopeKey },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isScopeSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GhostAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = GhostAmber,
                                        containerColor = DarkSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isScopeSelected,
                                        borderColor = if (isScopeSelected) GhostAmber else Color.Transparent
                                    )
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LocationCategory.entries.forEach { cat ->
                                val isCatSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = {
                                        selectedCategory = cat
                                    },
                                    label = {
                                        Text(
                                            text = cat.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PhosphorCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = PhosphorCyan,
                                        containerColor = DarkSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isCatSelected,
                                        borderColor = if (isCatSelected) PhosphorCyan else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Map Zoom & Recenter HUD Controls (Right edge)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.5f) },
                        modifier = Modifier.size(36.dp).testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = PhosphorCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(DarkSurfaceVariant)
                            .align(Alignment.CenterHorizontally)
                    )

                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.75f) },
                        modifier = Modifier.size(36.dp).testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = PhosphorCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(DarkSurfaceVariant)
                            .align(Alignment.CenterHorizontally)
                    )

                    IconButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        },
                        modifier = Modifier.size(36.dp).testTag("recenter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Recenter Map",
                            tint = SpectralEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Interactive Location Pin Popup Card (MANDATORY REQUIREMENT)
        AnimatedVisibility(
            visible = selectedLocation != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedLocation?.let { loc ->
                LocationPopupCard(
                    location = loc,
                    onClose = { onSelectLocation(null) },
                    onOpenDetailedPage = { onOpenDetails(loc) },
                    onOpenVoiceBox = { onOpenVoiceBox(loc) }
                )
            }
        }
    }
}

/**
 * Real-time HUD EMF detector component embedded directly on the map.
 * Continuously samples the device's hardware magnetometer sensor and calculates
 * anomalous field spikes and fluctuations as the investigator explores coordinates.
 */
@Composable
fun RealTimeEmfMapDetector(
    emfValue: Float,
    hasHardwareSensor: Boolean,
    hasSensorEvents: Boolean = false,
    nearestLocationName: String,
    proximityFactor: Float,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor by animateColorAsState(
        targetValue = when {
            emfValue > 80f -> WarningCrimson
            emfValue > 60f -> GhostAmber
            else -> SpectralEmerald
        },
        label = "emfAlertColor"
    )

    // Calculate active LED bar segments (5 stages)
    val ledSegments = 5
    val activeSegments = when {
        emfValue >= 85f -> 5
        emfValue >= 72f -> 4
        emfValue >= 60f -> 3
        emfValue >= 48f -> 2
        else -> 1
    }

    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .border(1.dp, alertColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onToggleExpand() }
            .testTag("realtime_emf_detector_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.94f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "EMF Sensor",
                        tint = alertColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REAL-TIME EMF DETECTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = alertColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PhosphorCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = when {
                                hasHardwareSensor && hasSensorEvents -> "HARDWARE LIVE"
                                hasHardwareSensor -> "SENSOR WAIT"
                                else -> "NO HW SENSOR"
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                hasHardwareSensor && hasSensorEvents -> PhosphorCyan
                                hasHardwareSensor -> GhostAmber
                                else -> WarningCrimson
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%.1f", emfValue)} µT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = alertColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle EMF details",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Real-time 5-stage LED Bar
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val segmentColors = listOf(
                    SpectralEmerald,
                    SpectralEmerald,
                    GhostAmber,
                    GhostAmber,
                    WarningCrimson
                )
                for (i in 0 until ledSegments) {
                    val isActive = i < activeSegments
                    val color = segmentColors[i]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) color else DarkSurface)
                            .border(0.5.dp, if (isActive) color else DarkSurfaceVariant, RoundedCornerShape(3.dp))
                    )
                }
            }

            // Expanded telemetry: nearest convergence & flux warning
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (proximityFactor > 0.4f) "HOTSPOT PROXIMITY: $nearestLocationName" else "PROXIMITY: AMBIENT PATROL",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (proximityFactor > 0.4f) GhostAmber else TextSecondary
                        )

                        Text(
                            text = when {
                                emfValue > 80f -> "CRITICAL SPIKE"
                                emfValue > 60f -> "ELEVATED FLUX"
                                else -> "NOMINAL"
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = alertColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich Popup Card displayed when clicking on an interactive pin.
 * Includes: Location name, brief description, thumbnail image, anomaly class badge,
 * and direct link/button to the detailed page.
 */

@Composable
fun LocationPopupCard(
    location: ShadowLocation,
    onClose: () -> Unit,
    onOpenDetailedPage: () -> Unit,
    onOpenVoiceBox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val classColor = when (location.anomalyClass) {
        AnomalyClass.CLASS_V_APPARITION -> WarningCrimson
        AnomalyClass.CLASS_IV_POLTERGEIST -> GhostAmber
        AnomalyClass.CLASS_III_ACTIVE -> SpectralEmerald
        AnomalyClass.CLASS_II_ATMOSPHERIC, AnomalyClass.CLASS_I_SUBTLE -> PhosphorCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .border(1.dp, classColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .testTag("location_popup_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Anomaly Class Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = classColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, classColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = location.anomalyClass.label.uppercase(),
                        color = classColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp).testTag("close_popup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close details popup",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Thumbnail + Title & Short Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail Image
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, PhosphorCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = location.drawableRes),
                        contentDescription = location.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().testTag("popup_thumbnail")
                    )
                }

                // Title and brief description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${location.municipality} • ${location.county}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PhosphorCyan,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (location.county != "Brevard County") {
                        Text(
                            text = "Road Trip: ${location.travelNote}",
                            color = GhostAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = location.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Coordinates and Link to Detailed Page
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.4f°N, %.4f°W", location.latitude, kotlin.math.abs(location.longitude)),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onOpenVoiceBox,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = DarkSurface,
                            contentColor = SpectralEmerald
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("popup_voice_box_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Open Spirit Voice Box for ${location.name}",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onOpenDetailedPage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PhosphorCyan,
                            contentColor = Color(0xFF002229)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_detailed_page_button")
                    ) {
                        Text(
                            text = "Open Detailed Dossier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View dossier details",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
