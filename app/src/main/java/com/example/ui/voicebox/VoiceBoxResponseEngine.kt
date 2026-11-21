package com.example.ui.voicebox

import com.example.data.model.AnomalyClass
import com.example.data.model.LocationCategory
import com.example.data.model.ShadowLocation
import java.util.Random

/**
 * Generates spirit transmissions and direct replies to investigator questions
 * grounded in the focused Brevard County site lore, EVP captures, and sensor flux.
 */
object VoiceBoxResponseEngine {

    private val rawSpiritWords = listOf(
        "COLD", "LISTEN", "BEHIND YOU", "STAY", "SHADOW", "REMEMBER",
        "DON'T LEAVE", "FOOTSTEPS", "WATCHING", "HERE", "HELP ME",
        "LANTERN", "WATER", "CYPRESS", "STATIC", "BREATHE", "CLOSER",
        "MIDNIGHT", "LOST", "LOCKED", "MIRROR", "WAIT", "LOOK UP",
        "WHO ARE YOU", "TURN BACK", "NO EXIT", "FOUND YOU", "STILL HERE"
    )

    private val ambientPhrases = listOf(
        "The air turned cold when you walked in...",
        "Do not turn off the sweep... we can speak through the static.",
        "I am standing near the edge of the trees.",
        "Too many footsteps tonight... stay quiet and listen.",
        "We never left this place when the lights went out.",
        "Watch the needle jump when I step closer to you.",
        "Someone else is pacing behind the doorway.",
        "Wait until midnight... the water remembers everything."
    )

    fun generateAmbientTransmission(
        location: ShadowLocation?,
        emfUt: Float,
        random: Random
    ): String {
        val locationFragments = buildLocationPhrases(location)
        val roll = random.nextInt(100)
        return when {
            emfUt > 72f && locationFragments.isNotEmpty() -> {
                locationFragments[random.nextInt(locationFragments.size)]
            }
            roll < 38 && locationFragments.isNotEmpty() -> {
                locationFragments[random.nextInt(locationFragments.size)]
            }
            roll < 70 -> {
                rawSpiritWords[random.nextInt(rawSpiritWords.size)]
            }
            else -> {
                ambientPhrases[random.nextInt(ambientPhrases.size)]
            }
        }
    }

    fun generateReplyForQuestion(
        question: String,
        location: ShadowLocation?,
        emfUt: Float,
        random: Random
    ): String {
        val q = question.lowercase()
        val locName = location?.name ?: "this sector"
        val town = location?.municipality ?: "Brevard County"

        return when {
            q.contains("who") || q.contains("name") -> {
                when (location?.id) {
                    "ashleys-rockledge" -> "Ethel... tell Jack I am still waiting by the Indian River."
                    "jim-morrison-house" -> "Just a poet passing through Vernon Place... listen to the organ."
                    "cocoa-playhouse" -> "Joe... checking the catwalk ropes before the curtain goes up."
                    "pritchard-house" -> "Miss Lovie... wipe your boots before stepping into my parlor."
                    "henegar-center" -> "Jonathan... want to hear my marbles roll down the stairs?"
                    "old-brevard-courthouse-gallows" -> "I stood beneath the courthouse oak... no one listened to my plea."
                    "fort-ann-haulover" -> "Private on watch at Haulover... wait for the morning relief boat."
                    "battle-of-poinsett" -> "Mounted patrol bogged down east of Lake Poinsett... stay on dry ground."
                    "crane-creek-turpentine-camp" -> "Still shackled by the creek trestle... listen to the iron links."
                    "windover-bog-titusville" -> "We rested beneath the dark peat for seven thousand winters."
                    "eau-gallie-hanging-oak" -> "Look up at the oak limb above the river... they gave no trial."
                    "harry-harriette-moore-mims" -> "Hold fast in Mims... our footsteps never left the citrus road."
                    else -> if (location != null) {
                        "We have lingered around $locName in $town far longer than you know."
                    } else {
                        "There are three of us standing just outside your lantern beam."
                    }
                }
            }

            q.contains("what happened") || q.contains("why") || q.contains("story") -> {
                when {
                    location != null && location.evpTranscript.isNotBlank() -> {
                        val cleaned = location.evpTranscript.trim().trim('"')
                        "Hear our echo: $cleaned"
                    }
                    location?.category == LocationCategory.MARSH_MARITIME -> {
                        "A sudden squall over the black water... no lantern reached us in time."
                    }
                    location?.category == LocationCategory.ABANDONED_RUINS -> {
                        "Smoke, ashes, and empty concrete left behind after everyone ran."
                    }
                    else -> {
                        "Old grief carved into the timber and soil of $town... it never faded."
                    }
                }
            }

            q.contains("safe") || q.contains("friendly") || q.contains("harm") || q.contains("leave") -> {
                when (location?.anomalyClass) {
                    AnomalyClass.CLASS_V_APPARITION -> {
                        "Do not linger past 3 AM... the boundary here grows restless and heavy."
                    }
                    AnomalyClass.CLASS_IV_POLTERGEIST -> {
                        "Respect the house rules and keep your distance... then you may listen."
                    }
                    else -> {
                        "We mean no harm... we only speak to those who pause long enough to hear."
                    }
                }
            }

            q.contains("sign") || q.contains("emf") || q.contains("meter") || q.contains("here") -> {
                val reading = String.format("%.1f", emfUt)
                "Watch your meter at $reading microtesla... I am standing right beside the receiver."
            }

            q.contains("how many") || q.contains("count") -> {
                val count = (location?.witnessCount ?: 3).coerceAtMost(9)
                "At least $count voices crowding this frequency right now... wait your turn."
            }

            else -> {
                val pool = buildLocationPhrases(location) + listOf(
                    "Your voice came through the static clearly... ask again before the band drifts.",
                    "Look toward the cold corner... something just crossed the floor.",
                    "Remember $town after midnight... the air shifts when we answer."
                )
                pool[random.nextInt(pool.size)]
            }
        }
    }

    private fun buildLocationPhrases(location: ShadowLocation?): List<String> {
        if (location == null) return emptyList()
        val evpClean = location.evpTranscript.trim().trim('"').trim('.').trim()
        val base = mutableListOf<String>()
        if (evpClean.isNotBlank()) {
            base.add(evpClean.replaceFirstChar { it.uppercase() })
        }
        base.add("${location.municipality.uppercase()}... CAN YOU HEAR ME?")
        when (location.category) {
            LocationCategory.ABANDONED_RUINS -> {
                base.add("No one comes down these empty streets anymore...")
                base.add("Dust and burnt masonry... stay out of the dark arches.")
            }
            LocationCategory.HISTORIC_HAUNTING -> {
                base.add("Step softly on the wooden stairs... someone is already upstairs.")
                base.add("Keep the parlor lights dim so we can stay.")
            }
            LocationCategory.CRYPTID_FOLKLORE -> {
                base.add("The frogs just went silent... something is moving through the palmettos.")
                base.add("Look between the moss branches before taking another step.")
            }
            LocationCategory.ROAD_ANOMALY -> {
                base.add("Watch the fog line ahead... phantom headlights are coming up fast.")
                base.add("Do not stop on the shoulder after two in the morning.")
            }
            LocationCategory.BATTLES_TRAGEDIES -> {
                base.add("Hear the drumbeat and iron links echo down the old trail...")
                base.add("Heavy sorrow clings to this ground from what took place here.")
            }
            LocationCategory.MARSH_MARITIME -> {
                base.add("The tide is turning across the lagoon... pull the skiff in.")
                base.add("Follow the pale lantern floating above the black water.")
            }
            LocationCategory.ALL -> {}
        }
        return base
    }
}
