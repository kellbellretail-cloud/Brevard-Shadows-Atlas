package com.example.ui.directory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationsDirectoryScreen(
    locations: List<ShadowLocation>,
    onSelectOnMap: (ShadowLocation) -> Unit,
    onOpenDetails: (ShadowLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LocationCategory.ALL) }
    var selectedSector by remember { mutableStateOf("All Brevard") }

    val sectorFilters = listOf(
        "All Brevard",
        "North Brevard",
        "Central Brevard",
        "South Brevard"
    )

    val filteredLocations = remember(locations, searchQuery, selectedCategory, selectedSector) {
        locations.filter { loc ->
            val matchesCategory = selectedCategory == LocationCategory.ALL || loc.category == selectedCategory
            val matchesSector = when (selectedSector) {
                "All Brevard" -> true
                "North Brevard" -> loc.latitude >= 28.53
                "Central Brevard" -> loc.latitude in 28.20..28.53
                "South Brevard" -> loc.latitude < 28.20
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    loc.name.contains(searchQuery, ignoreCase = true) ||
                    loc.municipality.contains(searchQuery, ignoreCase = true) ||
                    loc.shortDescription.contains(searchQuery, ignoreCase = true) ||
                    loc.fullLore.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSector && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightObsidian)
            .padding(horizontal = 14.dp)
            .testTag("locations_directory_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Brevard sites, battles, lore...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PhosphorCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = PhosphorCyan,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().testTag("directory_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Brevard County Sector Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sectorFilters.forEach { sectorName ->
                val isSectorSelected = selectedSector == sectorName
                val tagSlug = sectorName.lowercase().replace(" ", "_")
                FilterChip(
                    selected = isSectorSelected,
                    onClick = { selectedSector = sectorName },
                    label = {
                        Text(
                            text = sectorName,
                            fontSize = 11.sp,
                            fontWeight = if (isSectorSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GhostAmber.copy(alpha = 0.2f),
                        selectedLabelColor = GhostAmber,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSectorSelected,
                        borderColor = if (isSectorSelected) GhostAmber else Color.Transparent
                    ),
                    modifier = Modifier.testTag("sector_filter_$tagSlug")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LocationCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                        selected = isSelected,
                        borderColor = if (isSelected) PhosphorCyan else Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results Header
        Text(
            text = "CLASSIFIED DOSSIERS // ${filteredLocations.size} ENTRIES CATALOGED",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = PhosphorCyan,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("directory_list"),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLocations, key = { it.id }) { loc ->
                DirectoryLocationCard(
                    location = loc,
                    onLocateOnMap = { onSelectOnMap(loc) },
                    onOpenDossier = { onOpenDetails(loc) }
                )
            }
        }
    }
}

@Composable
fun DirectoryLocationCard(
    location: ShadowLocation,
    onLocateOnMap: () -> Unit,
    onOpenDossier: () -> Unit,
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
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .testTag("directory_card_${location.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${location.municipality} • ${location.county.removeSuffix(" County")}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (location.county != "Brevard County") {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GhostAmber.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GhostAmber.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "TRAVEL CORRIDOR • ${location.travelNote}",
                        color = GhostAmber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PhosphorCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                ) {
                    Image(
                        painter = painterResource(id = location.drawableRes),
                        contentDescription = location.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLocateOnMap,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PhosphorCyan
                    ),
                    modifier = Modifier.weight(1f).testTag("locate_map_btn_${location.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin on Map", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenDossier,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhosphorCyan,
                        contentColor = Color(0xFF002229)
                    ),
                    modifier = Modifier.weight(1f).testTag("open_dossier_btn_${location.id}")
                ) {
                    Text("Dossier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
