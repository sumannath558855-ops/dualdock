package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.data.CloneCatalog
import com.example.data.ClonedSpace
import com.example.data.DualAppPreset
import com.example.ui.theme.BentoCardPastels
import com.example.ui.theme.CanvasWhite
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.JetInk
import com.example.ui.theme.LavenderTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk

@Composable
fun BentoTopHeader(
    title: String,
    subtitle: String,
    isPinConfigured: Boolean,
    onLeftBadgeClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left circular brand / lock status avatar
        Surface(
            shape = CircleShape,
            color = LavenderTint,
            border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.65f)),
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onLeftBadgeClick)
                .testTag("header_lock_vault_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPinConfigured) Icons.Rounded.Shield else Icons.Rounded.Layers,
                    contentDescription = stringResource(R.string.cd_lock_vault),
                    tint = DeepLavender,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JetInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right circular outlined search button
        Surface(
            shape = CircleShape,
            color = PureWhite,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onSearchClick)
                .testTag("header_search_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_search_apps),
                    tint = JetInk,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

/**
 * Signature Soft-Lavender Hero Banner modeled after the playful 3D-ring Bento reference card.
 */
@Composable
fun LavenderInfinityBanner(
    totalClones: Int,
    clonedSpaces: List<ClonedSpace>,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PastelLavender)
            .clickable(onClick = onBannerClick)
            .testTag("lavender_hero_banner")
    ) {
        // Right-side sculptural glossy 3D pastel rings
        PastelRingsSculpture(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 164.dp, height = 168.dp)
                .offset(x = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.64f)) {
                Text(
                    text = "Unlimited\nclones",
                    style = MaterialTheme.typography.headlineLarge,
                    lineHeight = 31.sp,
                    color = JetInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (totalClones == 0) "Clone any app in one tap"
                    else "$totalClones active isolated spaces",
                    style = MaterialTheme.typography.bodySmall,
                    color = JetInk.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Overlapping circular mini-badges + deep purple "+∞" circle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewItems = if (clonedSpaces.isNotEmpty()) {
                    clonedSpaces.take(3).map { space ->
                        val preset = CloneCatalog.findByKey(space.presetKey)
                        val label = preset?.monogram ?: space.displayName.take(2).uppercase()
                        val color = preset?.let { Color(it.brandColorHex) } ?: Color(space.badgeColorHex)
                        label to color
                    }
                } else {
                    listOf(
                        "WA" to Color(0xFF25D366),
                        "FK" to Color(0xFF2874F0),
                        "MY" to Color(0xFFFF3F6C)
                    )
                }

                previewItems.forEachIndexed { idx, (code, color) ->
                    Box(
                        modifier = Modifier
                            .offset(x = (-8 * idx).dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = code,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = (-8 * previewItems.size).dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(DeepLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalClones > 3) "+${totalClones - 3}" else "+∞",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun PastelRingsSculpture(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun drawGlossyRing(
            cx: Float,
            cy: Float,
            outerR: Float,
            ringWidth: Float,
            baseColor: Color,
            highlightColor: Color
        ) {
            // Soft ambient drop shadow
            drawCircle(
                color = JetInk.copy(alpha = 0.14f),
                radius = outerR,
                center = Offset(cx + 4.dp.toPx(), cy + 6.dp.toPx())
            )
            // Main shaded torus body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(highlightColor, baseColor, baseColor.copy(alpha = 0.85f)),
                    center = Offset(cx - outerR * 0.28f, cy - outerR * 0.28f),
                    radius = outerR * 1.45f
                ),
                radius = outerR - ringWidth / 2f,
                center = Offset(cx, cy),
                style = Stroke(width = ringWidth)
            )
            // Glossy specular sheen arc
            drawCircle(
                color = Color.White.copy(alpha = 0.42f),
                radius = outerR * 0.22f,
                center = Offset(cx - outerR * 0.36f, cy - outerR * 0.36f)
            )
        }

        // Back layer orbs/rings
        drawGlossyRing(
            cx = w * 0.72f,
            cy = h * 0.27f,
            outerR = 26.dp.toPx(),
            ringWidth = 14.dp.toPx(),
            baseColor = Color(0xFFE0533C),
            highlightColor = Color(0xFFFF937D)
        )
        drawGlossyRing(
            cx = w * 0.75f,
            cy = h * 0.48f,
            outerR = 25.dp.toPx(),
            ringWidth = 14.dp.toPx(),
            baseColor = Color(0xFFEA8A24),
            highlightColor = Color(0xFFFFCA7A)
        )
        drawGlossyRing(
            cx = w * 0.67f,
            cy = h * 0.73f,
            outerR = 26.dp.toPx(),
            ringWidth = 15.dp.toPx(),
            baseColor = Color(0xFF1E5D55),
            highlightColor = Color(0xFF4CA89B)
        )
        // Middle cream + charcoal rings
        drawGlossyRing(
            cx = w * 0.50f,
            cy = h * 0.24f,
            outerR = 29.dp.toPx(),
            ringWidth = 16.dp.toPx(),
            baseColor = Color(0xFFEBE4D7),
            highlightColor = Color(0xFFFFFFFF)
        )
        drawGlossyRing(
            cx = w * 0.48f,
            cy = h * 0.76f,
            outerR = 26.dp.toPx(),
            ringWidth = 15.dp.toPx(),
            baseColor = Color(0xFFE4E0D8),
            highlightColor = Color(0xFFFFFFFF)
        )
        drawGlossyRing(
            cx = w * 0.35f,
            cy = h * 0.71f,
            outerR = 25.dp.toPx(),
            ringWidth = 15.dp.toPx(),
            baseColor = Color(0xFF282B3A),
            highlightColor = Color(0xFF565B75)
        )
        // Foreground hero sky-blue & candy-pink rings
        drawGlossyRing(
            cx = w * 0.36f,
            cy = h * 0.32f,
            outerR = 30.dp.toPx(),
            ringWidth = 17.dp.toPx(),
            baseColor = Color(0xFF58A6FF),
            highlightColor = Color(0xFFC2E2FF)
        )
        drawGlossyRing(
            cx = w * 0.25f,
            cy = h * 0.55f,
            outerR = 29.dp.toPx(),
            ringWidth = 16.dp.toPx(),
            baseColor = Color(0xFFE9709D),
            highlightColor = Color(0xFFFFBED6)
        )
    }
}

/**
 * Vertical oval capsule selector pill mirroring the Sun 11 .. Wed 14 (black) .. Sat 17 strip.
 */
@Composable
fun VerticalOvalCapsule(
    topLabel: String,
    bottomValue: String,
    selected: Boolean,
    hasIndicatorDot: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (selected) JetInk else PureWhite,
        border = if (selected) null else BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selected -> PureWhite
                            hasIndicatorDot -> JetInk
                            else -> Color.Transparent
                        }
                    )
            )
            Text(
                text = topLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) PureWhite.copy(alpha = 0.72f) else MutedInk,
                maxLines = 1
            )
            Text(
                text = bottomValue,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) PureWhite else JetInk,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BentoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = JetInk,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite
            )
        }
    }
}

@Composable
fun FrostedPastelTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = PureWhite.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = JetInk,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Reads an app's icon from LruCache immediately without blocking the UI thread,
 * and decodes it asynchronously on Dispatchers.IO if not yet cached.
 */
@Composable
fun rememberAsyncAppIcon(
    packageName: String,
    getCachedIcon: (String) -> ImageBitmap?,
    loadIcon: suspend (String) -> ImageBitmap?
): ImageBitmap? {
    val initial = remember(packageName) {
        if (packageName.isBlank()) null else getCachedIcon(packageName)
    }
    val loaded by produceState(initialValue = initial, key1 = packageName) {
        if (value == null && packageName.isNotBlank()) {
            value = loadIcon(packageName)
        }
    }
    return loaded
}

@Composable
fun BadgedAppIcon(
    displayName: String,
    presetKey: String?,
    appIconDrawable: Drawable? = null,
    cachedIconBitmap: ImageBitmap? = null,
    badgeText: String?,
    badgeColor: Color,
    iconSize: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val preset = remember(presetKey) { CloneCatalog.findByKey(presetKey) }
    val iconBitmap = cachedIconBitmap ?: remember(appIconDrawable) {
        appIconDrawable?.let {
            try {
                it.toBitmap(width = 128, height = 128).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val monogram = remember(preset, displayName) {
        preset?.monogram ?: displayName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "DD" }
    }

    val tileColor = remember(preset, badgeColor) {
        preset?.let { Color(it.brandColorHex) } ?: badgeColor
    }
    val badgeInk = if (badgeColor.luminance() > 0.55f) JetInk else PureWhite

    Box(
        modifier = modifier.size(iconSize + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (iconBitmap != null) PureWhite else tileColor,
            border = BorderStroke(2.dp, PureWhite),
            modifier = Modifier.size(iconSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(iconSize - 8.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = monogram,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }

        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText.take(2),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (badgeText.length <= 1) 10.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeInk,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PresetMonogramTile(
    preset: DualAppPreset,
    size: Dp = 44.dp
) {
    val brandColor = Color(preset.brandColorHex)
    Surface(
        shape = CircleShape,
        color = brandColor,
        border = BorderStroke(2.dp, PureWhite),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = preset.monogram,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        }
    }
}

/**
 * Colorful Pastel Bento Card for any cloned space in the unlimited grid.
 */
@Composable
fun PastelCloneBentoCard(
    space: ClonedSpace,
    pastelColor: Color,
    appIconDrawable: Drawable?,
    onLaunch: () -> Unit,
    onDuplicate: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = pastelColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onLaunch)
            .testTag("clone_card_${space.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrostedPastelTag(text = space.slotLabel)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (space.isLocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = stringResource(R.string.cd_clone_locked),
                            tint = JetInk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clone_more_${space.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.cd_clone_options),
                            tint = JetInk
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = space.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = JetInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (space.isWorkProfile) "Work Profile OS"
                    else if (space.isUnlimitedPool) "Unlimited Vault"
                    else "Isolated Process",
                    style = MaterialTheme.typography.bodySmall,
                    color = JetInk.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedAppIcon(
                    displayName = space.displayName,
                    presetKey = space.presetKey,
                    appIconDrawable = appIconDrawable,
                    badgeText = space.badgeText,
                    badgeColor = Color(space.badgeColorHex),
                    iconSize = 40.dp
                )

                if (space.isContainer) {
                    Surface(
                        shape = CircleShape,
                        color = PureWhite.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDuplicate)
                            .testTag("clone_duplicate_${space.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Duplicate ${space.displayName}",
                                tint = JetInk,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun pastelColorForIndex(index: Int): Color =
    BentoCardPastels[index % BentoCardPastels.size]

/**
 * Minimal launcher-style cloned app icon tile:
 * Rounded-square gradient-rimmed white app icon + small stacked clone number badge at bottom-right + centered app name below.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CloneLauncherIconTile(
    displayName: String,
    presetKey: String?,
    appIconDrawable: Drawable? = null,
    cachedIconBitmap: ImageBitmap? = null,
    badgeText: String,
    badgeColor: Color,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val preset = remember(presetKey) { CloneCatalog.findByKey(presetKey) }
    val iconBitmap = cachedIconBitmap ?: remember(appIconDrawable) {
        appIconDrawable?.let {
            try {
                it.toBitmap(width = 128, height = 128).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    val monogram = remember(preset, displayName) {
        preset?.monogram ?: displayName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "DD" }
    }
    val brandColor = remember(preset, badgeColor) {
        preset?.let { Color(it.brandColorHex) } ?: badgeColor
    }
    val iconFrameGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF3B84B), // Warm amber-gold
                Color(0xFF9ED49E), // Soft sage transition
                Color(0xFF72B7F8)  // Soft sky-blue
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rounded-square white app icon with signature pastel gradient frame
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(PureWhite)
                    .border(
                        width = 2.4.dp,
                        brush = iconFrameGradient,
                        shape = RoundedCornerShape(19.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else if (presetKey == "myntra") {
                    Text(
                        text = "M",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFFF8C21), Color(0xFFFF3F6C))
                            )
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(brandColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monogram,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite
                        )
                    }
                }
            }

            // Bottom-right stacked square clone badge (e.g. [2], [3], [4], [+])
            if (badgeText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PureWhite)
                        .padding(1.5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(
                            width = 1.3.dp,
                            color = Color(0xFFE99A52),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText.take(2),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (badgeText.length <= 1) 10.sp else 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97724),
                        maxLines = 1
                    )
                }
            }

            // Subtle lock indicator only when clone is locked
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = (-1).dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(JetInk),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.cd_clone_locked),
                        tint = PureWhite,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = JetInk,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Clean "+" launcher tile at the end of the Your spaces icon grid.
 */
@Composable
fun AddCloneLauncherTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("vault_grid_add_clone")
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(LavenderTint)
                    .border(
                        width = 1.5.dp,
                        color = PastelLavender,
                        shape = RoundedCornerShape(19.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_add_clone),
                    tint = DeepLavender,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = "Add",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = SlateInk,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
