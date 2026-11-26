package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CloneCatalog
import com.example.data.DualAppPreset
import com.example.system.InstalledAppInfo
import com.example.ui.DualDockUiState
import com.example.ui.components.BadgedAppIcon
import com.example.ui.components.FrostedPastelTag
import com.example.ui.components.PresetMonogramTile
import com.example.ui.components.StatusPill
import com.example.ui.components.pastelColorForIndex
import com.example.ui.components.rememberAsyncAppIcon
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.MintDeep
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SlateInk

@Composable
fun AppsPickerScreen(
    state: DualDockUiState,
    getCachedIconBitmap: (String) -> ImageBitmap?,
    loadIconBitmap: suspend (String) -> ImageBitmap?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onPresetClick: (DualAppPreset) -> Unit,
    onInstalledAppClick: (InstalledAppInfo) -> Unit,
    onCustomSpaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { listOf("All", "Shopping", "Messaging", "Social", "Delivery", "Productivity") }
    val query = state.appSearchQuery.trim()
    val filteredPresets = remember(query, state.selectedPresetCategory) {
        CloneCatalog.presets.filter { preset ->
            val categoryMatch = state.selectedPresetCategory == "All" ||
                preset.category.equals(state.selectedPresetCategory, ignoreCase = true)
            val queryMatch = query.isEmpty() ||
                preset.name.contains(query, ignoreCase = true) ||
                preset.category.contains(query, ignoreCase = true)
            categoryMatch && queryMatch
        }
    }
    val filteredInstalled = remember(query, state.installedApps) {
        if (query.isEmpty()) state.installedApps
        else state.installedApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }
    val workPackageSet = remember(state.workProfileStatus.workApps) {
        state.workProfileStatus.workApps.mapTo(HashSet()) { it.packageName }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Clone Studio",
                        style = MaterialTheme.typography.headlineLarge,
                        color = JetInk
                    )
                    StatusPill(text = "∞ UNLIMITED", accent = DeepLavender)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create unlimited isolated clones of any app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateInk
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = state.appSearchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search WhatsApp, Flipkart, Myntra, or installed apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search_apps),
                            tint = JetInk
                        )
                    },
                    trailingIcon = {
                        if (state.appSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                    tint = SlateInk
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JetInk,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input")
                )
            }
        }

        // Soft Lavender Custom Unlimited Space Banner
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = PastelLavender,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(onClick = onCustomSpaceClick)
                    .testTag("custom_web_space_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PureWhite,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = DeepLavender
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Unlimited Space",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JetInk
                        )
                        Text(
                            text = "Clone any URL or shopping site endlessly",
                            style = MaterialTheme.typography.bodySmall,
                            color = JetInk.copy(alpha = 0.72f)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = JetInk,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Pill Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val selected = state.selectedPresetCategory == cat
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JetInk,
                            selectedLabelColor = PureWhite,
                            containerColor = PureWhite,
                            labelColor = SlateInk
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = JetInk
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                    )
                }
            }
        }

        // Popular Dual Apps Pastel Bento Row
        if (filteredPresets.isNotEmpty()) {
            item {
                Text(
                    text = "Popular apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = JetInk
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredPresets, key = { _, it -> it.key }) { idx, preset ->
                        val existingClones = state.cloneCountForPreset(preset.key)
                        val cardColor = pastelColorForIndex(idx)
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = cardColor,
                            modifier = Modifier
                                .width(154.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable { onPresetClick(preset) }
                                .testTag("preset_card_${preset.key}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FrostedPastelTag(text = preset.category)
                                    Surface(
                                        shape = CircleShape,
                                        color = JetInk,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Clone ${preset.name}",
                                                tint = PureWhite,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PresetMonogramTile(preset = preset, size = 38.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = JetInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (existingClones > 0) "$existingClones cloned" else "+ Clone",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = JetInk.copy(alpha = 0.72f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Installed Phone Apps
        item {
            Text(
                text = "Installed on your phone (${filteredInstalled.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = JetInk
            )
        }

        if (state.isLoadingApps) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = JetInk)
                }
            }
        } else if (filteredInstalled.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No installed apps matched \"$query\". Tap Custom Unlimited Space above to clone any app URL.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateInk,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = filteredInstalled,
                key = { it.packageName },
                contentType = { "installed_app_row" }
            ) { app ->
                val isWorkCloned = workPackageSet.contains(app.packageName)
                val iconBitmap = rememberAsyncAppIcon(
                    packageName = app.packageName,
                    getCachedIcon = getCachedIconBitmap,
                    loadIcon = loadIconBitmap
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onInstalledAppClick(app) }
                        .testTag("installed_app_${app.packageName}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BadgedAppIcon(
                            displayName = app.label,
                            presetKey = app.matchedPresetKey,
                            cachedIconBitmap = iconBitmap,
                            badgeText = null,
                            badgeColor = DeepLavender,
                            iconSize = 42.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = JetInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isWorkCloned) {
                            StatusPill(text = "WORK OS", accent = SkyDeep)
                        } else if (app.matchedPresetKey != null) {
                            StatusPill(text = "INSTANT", accent = MintDeep)
                        } else {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = JetInk
                            ) {
                                Text(
                                    text = "+ Clone",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
