package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ShadowLocationsRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.voicebox.VoiceBoxBand
import com.example.ui.voicebox.VoiceBoxEntry
import com.example.ui.voicebox.VoiceBoxResponseEngine
import com.example.ui.voicebox.VoiceBoxScreen
import com.example.ui.voicebox.VoiceBoxSpeaker
import com.example.ui.voicebox.VoiceBoxUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Brevard Shadow Atlas", appName)
  }

  @Test
  fun `voice box screen renders physical spirit write-out and band selector`() {
    var selectedBand = VoiceBoxBand.FM_SWEEP
    val sampleLocation = ShadowLocationsRepository.locations.first { it.id == "ashleys-rockledge" }

    composeTestRule.setContent {
      MyApplicationTheme {
        VoiceBoxScreen(
          uiState = VoiceBoxUiState(
            isActive = true,
            isWriting = true,
            currentlyWritingText = "Ethel... still by the ri",
            currentlyWritingFreqMhz = 104.1f,
            currentlyWritingEmfUt = 72.5f,
            currentFrequencyMhz = 104.1f,
            liveEmfUt = 72.5f,
            statusLine = "SPIRIT BREAKTHROUGH • WRITING..."
          ),
          contextLocation = sampleLocation,
          availableLocations = ShadowLocationsRepository.locations,
          entries = listOf(
            VoiceBoxEntry(
              id = "1",
              speaker = VoiceBoxSpeaker.SPIRIT,
              text = "COLD RIVER",
              frequencyMhz = 104.1f,
              emfMicroTesla = 68.4f
            )
          ),
          onSelectBand = { selectedBand = it },
          onSelectFocusLocation = {},
          onToggleStaticAudio = {},
          onToggleSpokenVoice = {},
          onStartVoiceBox = {},
          onStopVoiceBox = {},
          onAskSpiritsQuestion = {},
          onSaveTapeToLogbook = {},
          onClearEntries = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("voice_box_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("spirit_writer_display").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ethel... still by the ri █").assertIsDisplayed()
    composeTestRule.onNodeWithText("COLD RIVER").assertIsDisplayed()
    composeTestRule.onNodeWithTag("band_chip_vhf_band").performClick()
    assertEquals(VoiceBoxBand.VHF_BAND, selectedBand)

    val reply = VoiceBoxResponseEngine.generateReplyForQuestion(
      question = "Who is here?",
      location = sampleLocation,
      emfUt = 72.5f,
      random = Random(42)
    )
    assertTrue(reply.contains("Ethel", ignoreCase = true))
  }

  @Test
  fun `atlas catalog stays strictly within Brevard County and includes event sites`() {
    val locations = ShadowLocationsRepository.locations
    assertTrue(locations.isNotEmpty())
    assertEquals(locations.size, locations.map { it.id }.distinct().size)

    locations.forEach { loc ->
      assertEquals("${loc.id} must be in Brevard County", "Brevard County", loc.county)
      assertTrue("${loc.id} lat out of Brevard bounds", loc.latitude in 27.80..28.80)
      assertTrue("${loc.id} lng out of Brevard bounds", loc.longitude in -80.98..-80.40)
    }

    val ids = locations.map { it.id }.toSet()
    assertTrue(ids.contains("old-brevard-courthouse-gallows"))
    assertTrue(ids.contains("fort-ann-haulover"))
    assertTrue(ids.contains("battle-of-poinsett"))
    assertTrue(ids.contains("crane-creek-turpentine-camp"))
    assertTrue(ids.contains("windover-bog-titusville"))
    assertTrue(ids.contains("eau-gallie-hanging-oak"))
  }

  @Test
  fun `directory filters Brevard County sectors and event categories`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.directory.LocationsDirectoryScreen(
          locations = ShadowLocationsRepository.locations,
          onSelectOnMap = {},
          onOpenDetails = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("locations_directory_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sector_filter_north_brevard").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("directory_list")
      .performScrollToNode(hasTestTag("directory_card_old-brevard-courthouse-gallows"))
    composeTestRule.onNodeWithTag("directory_card_old-brevard-courthouse-gallows").assertIsDisplayed()
  }

  @Test
  fun `main navigation removes tools tab emf meter and spirit box sweep`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ShadowAtlasApp()
      }
    }

    composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_map").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_directory").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_voice_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_journal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_tools").assertDoesNotExist()
    composeTestRule.onNodeWithText("TOOLS").assertDoesNotExist()
    composeTestRule.onNodeWithText("EMF METER").assertDoesNotExist()
    composeTestRule.onNodeWithText("SPIRIT BOX SWEEP").assertDoesNotExist()
  }
}
