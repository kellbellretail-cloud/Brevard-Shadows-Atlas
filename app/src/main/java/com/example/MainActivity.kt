package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.detail.LocationDetailScreen
import com.example.ui.directory.LocationsDirectoryScreen
import com.example.ui.journal.InvestigationJournalScreen
import com.example.ui.map.InteractiveAtlasMap
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PhosphorCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.voicebox.VoiceBoxScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ShadowAtlasApp()
            }
        }
    }
}

@Composable
fun ShadowAtlasApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedLocationForMap by viewModel.selectedLocationForMap.collectAsState()
    val activeDetailLocation by viewModel.activeDetailLocation.collectAsState()
    val investigationLogs by viewModel.investigationLogs.collectAsState()

    val voiceBoxUiState by viewModel.voiceBoxUiState.collectAsState()
    val voiceBoxContextLocation by viewModel.voiceBoxContextLocation.collectAsState()
    val voiceBoxEntries by viewModel.voiceBoxEntries.collectAsState()

    // If viewing a detailed location dossier, show the detail screen
    if (activeDetailLocation != null) {
        val loc = activeDetailLocation!!
        LocationDetailScreen(
            location = loc,
            onBack = { viewModel.closeDetailedPage() },
            onSaveInvestigation = { log -> viewModel.saveInvestigationLog(log) },
            onOpenVoiceBox = { viewModel.openVoiceBoxForLocation(loc) }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("shadow_atlas_scaffold"),
            containerColor = MidnightObsidian,
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = PhosphorCyan,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.ATLAS_MAP,
                        onClick = { viewModel.switchTab(AppTab.ATLAS_MAP) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Interactive Radar Map"
                            )
                        },
                        label = {
                            Text(
                                text = "MAP",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MidnightObsidian,
                            selectedTextColor = PhosphorCyan,
                            indicatorColor = PhosphorCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_map")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.DIRECTORY,
                        onClick = { viewModel.switchTab(AppTab.DIRECTORY) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = "Location Dossiers"
                            )
                        },
                        label = {
                            Text(
                                text = "DOSSIERS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MidnightObsidian,
                            selectedTextColor = PhosphorCyan,
                            indicatorColor = PhosphorCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_directory")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.VOICE_BOX,
                        onClick = { viewModel.switchTab(AppTab.VOICE_BOX) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Spirit Voice Box"
                            )
                        },
                        label = {
                            Text(
                                text = "VOICE BOX",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MidnightObsidian,
                            selectedTextColor = PhosphorCyan,
                            indicatorColor = PhosphorCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_voice_box")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.JOURNAL,
                        onClick = { viewModel.switchTab(AppTab.JOURNAL) },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = "Field Logbook"
                            )
                        },
                        label = {
                            Text(
                                text = "LOGBOOK",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MidnightObsidian,
                            selectedTextColor = PhosphorCyan,
                            indicatorColor = PhosphorCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_journal")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContentAnimation"
                ) { tab ->
                    when (tab) {
                        AppTab.ATLAS_MAP -> {
                            InteractiveAtlasMap(
                                locations = viewModel.locations,
                                selectedLocation = selectedLocationForMap,
                                onSelectLocation = { loc -> viewModel.selectLocationOnMap(loc) },
                                onOpenDetails = { loc -> viewModel.openDetailedPage(loc) },
                                onOpenVoiceBox = { loc -> viewModel.openVoiceBoxForLocation(loc) }
                            )
                        }
                        AppTab.DIRECTORY -> {
                            LocationsDirectoryScreen(
                                locations = viewModel.locations,
                                onSelectOnMap = { loc -> viewModel.locateAndFocusOnMap(loc) },
                                onOpenDetails = { loc -> viewModel.openDetailedPage(loc) }
                            )
                        }
                        AppTab.VOICE_BOX -> {
                            VoiceBoxScreen(
                                uiState = voiceBoxUiState,
                                contextLocation = voiceBoxContextLocation,
                                availableLocations = viewModel.locations,
                                entries = voiceBoxEntries,
                                onSelectBand = { band -> viewModel.selectVoiceBoxBand(band) },
                                onSelectFocusLocation = { loc -> viewModel.setVoiceBoxFocusLocation(loc) },
                                onToggleStaticAudio = { viewModel.toggleVoiceBoxStaticAudio() },
                                onToggleSpokenVoice = { viewModel.toggleVoiceBoxSpokenOutput() },
                                onStartVoiceBox = { hasMic -> viewModel.startVoiceBox(hasMic) },
                                onStopVoiceBox = { viewModel.stopVoiceBox() },
                                onAskSpiritsQuestion = { q -> viewModel.askSpiritsQuestion(q) },
                                onSaveTapeToLogbook = { viewModel.saveVoiceBoxTapeToLogbook() },
                                onClearEntries = { viewModel.clearVoiceBoxEntries() }
                            )
                        }
                        AppTab.JOURNAL -> {
                            InvestigationJournalScreen(
                                logs = investigationLogs,
                                onDeleteLog = { log -> viewModel.deleteInvestigationLog(log) }
                            )
                        }
                    }
                }
            }
        }
    }
}
