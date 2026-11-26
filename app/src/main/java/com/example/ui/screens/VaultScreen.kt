package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CloneCatalog
import com.example.data.ClonedSpace
import com.example.data.DualAppPreset
import com.example.ui.DualDockUiState
import com.example.ui.VaultFilter
import com.example.ui.components.AddCloneLauncherTile
import com.example.ui.components.BentoTopHeader
import com.example.ui.components.CloneLauncherIconTile
import com.example.ui.components.LavenderInfinityBanner
import com.example.ui.components.VerticalOvalCapsule
import com.example.ui.components.rememberAsyncAppIcon
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk

private sealed interface SpaceGridItem {
    val stableKey: String
    data class Cloned(val space: ClonedSpace) : SpaceGridItem {
        override val stableKey: String = "clone_${space.id}"
    }
    data class QuickPreset(val preset: DualAppPreset) : SpaceGridItem {
        override val stableKey: String = "preset_${preset.key}"
    }
    data object AddTile : SpaceGridItem {
        override val stableKey: String = "add_tile"
    }
}

@Composable
fun VaultScreen(
    state: DualDockUiState,
    getCachedIconBitmap: (String) -> ImageBitmap?,
    loadIconBitmap: suspend (String) -> ImageBitmap?,
    onSelectFilter: (VaultFilter) -> Unit,
    onLaunchSpace: (ClonedSpace) -> Unit,
    onDuplicateSpace: (ClonedSpace) -> Unit,
    onMoreSpace: (ClonedSpace) -> Unit,
    onQuickPresetClick: (DualAppPreset) -> Unit,
    onOpenAppPicker: () -> Unit,
    onLockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spaces = state.filteredSpaces
    val whatsappPreset = CloneCatalog.findByKey("whatsapp") ?: CloneCatalog.presets[0]
    val flipkartPreset = CloneCatalog.findByKey("flipkart") ?: CloneCatalog.presets[1]
    val myntraPreset = CloneCatalog.findByKey("myntra") ?: CloneCatalog.presets[2]

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 560.dp) 5 else 3

        val gridItems: List<SpaceGridItem> = when {
            spaces.isNotEmpty() -> {
                spaces.map { SpaceGridItem.Cloned(it) } + SpaceGridItem.AddTile
            }
            state.vaultFilter == VaultFilter.ALL -> {
                listOf(
                    SpaceGridItem.QuickPreset(myntraPreset),
                    SpaceGridItem.QuickPreset(flipkartPreset),
                    SpaceGridItem.QuickPreset(whatsappPreset),
                    SpaceGridItem.AddTile
                )
            }
            else -> emptyList()
        }
        val gridRows = gridItems.chunked(columns)

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Clean Centered Top Header (Avatar | Greeting + Subtitle | Outlined Search Circle)
            item {
                BentoTopHeader(
                    title = "Hello, DualDock",
                    subtitle = if (state.totalClonesCount == 0) "Unlimited Clones Ready"
                    else "${state.totalClonesCount} Spaces • Unlimited Ready",
                    isPinConfigured = state.securitySettings.hasPin,
                    onLeftBadgeClick = onLockClick,
                    onSearchClick = onOpenAppPicker
                )
            }

            // 2. Signature Soft-Lavender Hero Banner with 3D Pastel Rings & Overlapping Avatars
            item {
                LavenderInfinityBanner(
                    totalClones = state.totalClonesCount,
                    clonedSpaces = state.clonedSpaces,
                    onBannerClick = onOpenAppPicker
                )
            }

            // 3. Signature Row of Vertical Oval Capsules (All | Slots | Work | Lock | Clone +)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerticalOvalCapsule(
                        topLabel = "All",
                        bottomValue = state.totalClonesCount.toString(),
                        selected = state.vaultFilter == VaultFilter.ALL,
                        hasIndicatorDot = state.totalClonesCount > 0,
                        onClick = { onSelectFilter(VaultFilter.ALL) },
                        testTag = "vault_filter_all",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalOvalCapsule(
                        topLabel = "Slots",
                        bottomValue = state.clonedSpaces.count { it.isContainer }.toString(),
                        selected = state.vaultFilter == VaultFilter.CONTAINERS,
                        hasIndicatorDot = state.clonedSpaces.any { it.isContainer },
                        onClick = { onSelectFilter(VaultFilter.CONTAINERS) },
                        testTag = "vault_filter_containers",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalOvalCapsule(
                        topLabel = "Work",
                        bottomValue = state.clonedSpaces.count { it.isWorkProfile }.toString(),
                        selected = state.vaultFilter == VaultFilter.WORK,
                        hasIndicatorDot = state.workProfileStatus.hasWorkProfile,
                        onClick = { onSelectFilter(VaultFilter.WORK) },
                        testTag = "vault_filter_work",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalOvalCapsule(
                        topLabel = "Lock",
                        bottomValue = state.lockedClonesCount.toString(),
                        selected = state.vaultFilter == VaultFilter.LOCKED,
                        hasIndicatorDot = state.lockedClonesCount > 0,
                        onClick = { onSelectFilter(VaultFilter.LOCKED) },
                        testTag = "vault_filter_locked",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalOvalCapsule(
                        topLabel = "Clone",
                        bottomValue = "+",
                        selected = false,
                        hasIndicatorDot = true,
                        onClick = onOpenAppPicker,
                        testTag = "vault_quick_add_capsule",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Section Title ("Your spaces") + Hold-for-options hint
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your spaces",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = JetInk
                        )
                        if (spaces.isNotEmpty()) {
                            Text(
                                text = "Tap to open • Hold icon for options",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateInk
                            )
                        }
                    }
                    Text(
                        text = "See all →",
                        style = MaterialTheme.typography.labelLarge,
                        color = SlateInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenAppPicker)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("vault_see_all_apps")
                    )
                }
            }

            // 5. Minimal App-Icon-Only Grid (3 columns on phone, 5 on tablet)
            if (gridRows.isNotEmpty()) {
                items(
                    count = gridRows.size,
                    key = { rowIdx -> gridRows[rowIdx].joinToString("|") { it.stableKey } },
                    contentType = { "space_icon_row" }
                ) { rowIndex ->
                    val rowItems = gridRows[rowIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { entry ->
                            when (entry) {
                                is SpaceGridItem.Cloned -> {
                                    val space = entry.space
                                    val iconBitmap = rememberAsyncAppIcon(
                                        packageName = space.packageName,
                                        getCachedIcon = getCachedIconBitmap,
                                        loadIcon = loadIconBitmap
                                    )
                                    CloneLauncherIconTile(
                                        displayName = space.displayName,
                                        presetKey = space.presetKey,
                                        cachedIconBitmap = iconBitmap,
                                        badgeText = space.badgeText,
                                        badgeColor = Color(space.badgeColorHex),
                                        isLocked = space.isLocked,
                                        onClick = { onLaunchSpace(space) },
                                        onLongClick = { onMoreSpace(space) },
                                        testTag = "clone_card_${space.id}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                is SpaceGridItem.QuickPreset -> {
                                    val preset = entry.preset
                                    val pkg = preset.knownPackageNames.firstOrNull().orEmpty()
                                    val iconBitmap = rememberAsyncAppIcon(
                                        packageName = pkg,
                                        getCachedIcon = getCachedIconBitmap,
                                        loadIcon = loadIconBitmap
                                    )
                                    CloneLauncherIconTile(
                                        displayName = "${preset.name} +",
                                        presetKey = preset.key,
                                        cachedIconBitmap = iconBitmap,
                                        badgeText = "+",
                                        badgeColor = Color(preset.brandColorHex),
                                        isLocked = false,
                                        onClick = { onQuickPresetClick(preset) },
                                        onLongClick = null,
                                        testTag = "quick_clone_${preset.key}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                SpaceGridItem.AddTile -> {
                                    AddCloneLauncherTile(
                                        onClick = onOpenAppPicker,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No clones in ${state.vaultFilter.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = JetInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap All above or tap Clone to add a new app space.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateInk
                            )
                        }
                    }
                }
            }
        }
    }
}
