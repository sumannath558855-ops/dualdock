package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.system.WorkProfileAppInfo
import com.example.ui.DualDockUiState
import com.example.ui.components.BadgedAppIcon
import com.example.ui.components.BentoPrimaryButton
import com.example.ui.components.FrostedPastelTag
import com.example.ui.components.rememberAsyncAppIcon
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.MintDeep
import com.example.ui.theme.PastelAmber
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SlateInk

@Composable
fun WorkSpaceScreen(
    state: DualDockUiState,
    getCachedIconBitmap: (String) -> ImageBitmap?,
    loadIconBitmap: suspend (String) -> ImageBitmap?,
    onSetupWorkProfile: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenWorkPlayStore: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchWorkApp: (WorkProfileAppInfo) -> Unit,
    onImportWorkApp: (WorkProfileAppInfo) -> Unit,
    onJumpToContainers: () -> Unit,
    onQuickNativeClone: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val work = state.workProfileStatus

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Work Space",
                        style = MaterialTheme.typography.headlineLarge,
                        color = JetInk
                    )
                    Text(
                        text = "OS-level Android Work Profile for native APKs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateInk
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = PureWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier.testTag("refresh_work_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Work Profile status",
                            tint = JetInk
                        )
                    }
                }
            }
        }

        // Sky-Blue Pastel Work Profile Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PastelSky,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FrostedPastelTag(
                            text = if (work.isManagedUsersSupported) "OS SUPPORTED" else "OS RESTRICTED"
                        )
                        if (work.hasWorkProfile) {
                            FrostedPastelTag(
                                text = if (work.isWorkProfilePaused) "PAUSED" else "ACTIVE"
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (work.hasWorkProfile) Icons.Rounded.CheckCircle else Icons.Rounded.WorkOutline,
                                    contentDescription = null,
                                    tint = SkyDeep,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    work.hasWorkProfile && work.isWorkProfilePaused -> "Work Profile Paused"
                                    work.hasWorkProfile -> "Work Profile Active"
                                    work.isManagedUsersSupported -> "Work Profile Ready"
                                    else -> "Single-Profile Device"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = JetInk
                            )
                            Text(
                                text = when {
                                    work.hasWorkProfile -> "${work.workApps.size} native apps detected inside Android's isolated Work User."
                                    work.isManagedUsersSupported -> "Create a second OS user profile to run a separate native installation of WhatsApp, Myntra, Flipkart, or any Play Store app."
                                    else -> "This device restricts Managed Profiles. Use Unlimited Clone Studio for Flipkart, Myntra, WhatsApp, and any web app."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = JetInk.copy(alpha = 0.78f)
                            )
                        }
                    }

                    if (!work.hasWorkProfile) {
                        if (work.isManagedUsersSupported) {
                            BentoPrimaryButton(
                                text = "Create Native Work Profile",
                                icon = Icons.Rounded.WorkOutline,
                                onClick = onSetupWorkProfile,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "setup_work_profile_button"
                            )
                        }
                        OutlinedButton(
                            onClick = onJumpToContainers,
                            border = BorderStroke(1.2.dp, JetInk.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("work_fallback_containers_button")
                        ) {
                            Text("Open Unlimited Clone Studio", color = JetInk)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BentoPrimaryButton(
                                text = "Work Play Store",
                                icon = Icons.Rounded.Shop,
                                onClick = onOpenWorkPlayStore,
                                modifier = Modifier.weight(1f),
                                testTag = "open_work_play_store_button"
                            )
                            Surface(
                                shape = CircleShape,
                                color = PureWhite,
                                modifier = Modifier.size(52.dp)
                            ) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.testTag("open_work_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Work Profile Settings",
                                        tint = JetInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mint Native WhatsApp-OTP Quick Clones Card
        item {
            val popularNativeApps = listOf(
                Triple("Myntra", "com.myntra.android", "Get OTP on WhatsApp"),
                Triple("Flipkart", "com.flipkart.android", "Native Login & Cart"),
                Triple("WhatsApp", "com.whatsapp", "Second Phone Number"),
                Triple("Amazon", "in.amazon.mShop.android.shopping", "Native Prime App")
            )
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = PastelMint,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FrostedPastelTag(text = "FULL NATIVE FEATURES • WHATSAPP OTP")
                    Text(
                        text = "Native App Clones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JetInk
                    )
                    Text(
                        text = "Install the real Play Store app inside Work Space to get native features like Myntra's 'Get OTP on WhatsApp' and SMS auto-fill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JetInk.copy(alpha = 0.78f)
                    )
                    popularNativeApps.forEach { (label, pkg, hint) ->
                        val isInstalledInWork = work.workApps.any { it.packageName == pkg }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PureWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onQuickNativeClone(label, pkg) }
                                .testTag("quick_native_clone_$pkg")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = JetInk
                                    )
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateInk
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isInstalledInWork) MintDeep else JetInk
                                ) {
                                    Text(
                                        text = if (isInstalledInWork) "Open" else "Install",
                                        style = MaterialTheme.typography.labelSmall,
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

        // Warm Amber How-It-Works Bento Card
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = PastelAmber,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrostedPastelTag(text = "HOW IT WORKS")
                    Text(
                        text = "True Native App Separation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JetInk
                    )
                    Text(
                        text = "1. Android creates a second OS user profile with isolated data and notifications.\n" +
                            "2. Install any native app (Myntra, Flipkart, WhatsApp) via Work Play Store for full native WhatsApp OTP.\n" +
                            "3. Import cloned Work apps into DualDock for your clean app icon grid, shortcuts, and PIN lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JetInk.copy(alpha = 0.78f)
                    )
                }
            }
        }

        if (work.hasWorkProfile) {
            item {
                Text(
                    text = "Apps in Work Profile (${work.workApps.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = JetInk
                )
            }

            if (work.workApps.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No apps found inside the Work Profile yet. Open Work Play Store above to install a second copy of any app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateInk,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(work.workApps, key = { it.packageName }) { app ->
                    val alreadyInVault = state.clonedSpaces.any {
                        it.isWorkProfile && it.packageName == app.packageName
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val iconBitmap = rememberAsyncAppIcon(
                                packageName = app.packageName,
                                getCachedIcon = getCachedIconBitmap,
                                loadIcon = loadIconBitmap
                            )
                            BadgedAppIcon(
                                displayName = app.label,
                                presetKey = null,
                                cachedIconBitmap = iconBitmap,
                                badgeText = "W",
                                badgeColor = SkyDeep,
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

                            IconButton(
                                onClick = { onLaunchWorkApp(app) },
                                modifier = Modifier.testTag("launch_work_app_${app.packageName}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Launch ${app.label}",
                                    tint = SkyDeep
                                )
                            }

                            if (!alreadyInVault) {
                                IconButton(
                                    onClick = { onImportWorkApp(app) },
                                    modifier = Modifier.testTag("import_work_app_${app.packageName}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add ${app.label} to Vault",
                                        tint = MintDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
