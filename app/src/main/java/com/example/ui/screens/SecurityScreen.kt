package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ClonedSpace
import com.example.system.BiometricAvailability
import com.example.ui.DualDockUiState
import com.example.ui.components.BadgedAppIcon
import com.example.ui.components.BentoPrimaryButton
import com.example.ui.components.FrostedPastelTag
import com.example.ui.components.rememberAsyncAppIcon
import com.example.ui.theme.AmberDeep
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SoftCloud

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityScreen(
    state: DualDockUiState,
    getCachedIconBitmap: (String) -> ImageBitmap?,
    loadIconBitmap: suspend (String) -> ImageBitmap?,
    onSetupOrChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onToggleVaultLock: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSelectAutoLockTimeout: (Int) -> Unit,
    onLockNow: () -> Unit,
    onToggleSpaceLock: (ClonedSpace) -> Unit,
    modifier: Modifier = Modifier
) {
    val security = state.securitySettings
    val timeoutOptions = listOf(
        0 to "Immediate",
        60 to "1 Min",
        300 to "5 Min",
        -1 to "Manual"
    )

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.headlineLarge,
                    color = JetInk
                )
                Text(
                    text = "PIN & biometric lock for DualDock and Home Screen shortcuts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateInk
                )
            }
        }

        // Soft Lavender Privacy Lock Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PastelLavender,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FrostedPastelTag(
                            text = if (security.hasPin) "${security.pinLength}-DIGIT PIN ACTIVE" else "NOT LOCKED"
                        )
                        Switch(
                            checked = security.isVaultLockEnabled,
                            onCheckedChange = onToggleVaultLock,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = JetInk,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = PureWhite.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.testTag("master_vault_lock_switch")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = DeepLavender
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Vault Privacy Lock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = JetInk
                            )
                            Text(
                                text = if (security.hasPin) "Require PIN or biometrics to enter DualDock"
                                else "Set a Vault PIN to protect your cloned accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = JetInk.copy(alpha = 0.74f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoPrimaryButton(
                            text = if (security.hasPin) "Change PIN" else "Set Vault PIN",
                            icon = Icons.Rounded.Key,
                            onClick = onSetupOrChangePin,
                            modifier = Modifier.weight(1f),
                            testTag = "setup_or_change_pin_button"
                        )

                        if (security.hasPin) {
                            OutlinedButton(
                                onClick = onLockNow,
                                border = BorderStroke(1.2.dp, JetInk.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("lock_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Lock now",
                                    tint = JetInk,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lock", color = JetInk)
                            }
                        }
                    }

                    if (security.hasPin) {
                        TextButton(
                            onClick = onRemovePin,
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("remove_pin_button")
                        ) {
                            Text("Remove Vault PIN", color = DangerCoral)
                        }
                    }
                }
            }
        }

        // Clean White Biometric & Auto-Lock Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SoftCloud,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = DeepLavender,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Biometric Unlock",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JetInk
                                )
                                Text(
                                    text = when (state.biometricAvailability) {
                                        BiometricAvailability.AVAILABLE -> "Fingerprint / Face unlock ready"
                                        BiometricAvailability.NONE_ENROLLED -> "Enroll fingerprint or face in Android Settings"
                                        BiometricAvailability.UNAVAILABLE -> "Biometric hardware unavailable on this device"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateInk
                                )
                            }
                        }
                        Switch(
                            checked = security.isBiometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            enabled = state.biometricAvailability == BiometricAvailability.AVAILABLE,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = JetInk
                            ),
                            modifier = Modifier.testTag("biometric_unlock_switch")
                        )
                    }

                    HorizontalDivider(
                        color = HairlineBorder,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = SlateInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Auto-Lock When Leaving DualDock",
                            style = MaterialTheme.typography.titleSmall,
                            color = JetInk
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeoutOptions.forEach { (seconds, label) ->
                            val selected = security.autoLockTimeoutSeconds == seconds
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectAutoLockTimeout(seconds) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JetInk,
                                    selectedLabelColor = PureWhite,
                                    containerColor = SoftCloud,
                                    labelColor = SlateInk
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = HairlineBorder,
                                    selectedBorderColor = JetInk
                                ),
                                modifier = Modifier.testTag("autolock_$seconds")
                            )
                        }
                    }
                }
            }
        }

        // Per-Clone Lock Controls
        item {
            Text(
                text = "Per-clone lock (${state.clonedSpaces.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = JetInk
            )
        }

        if (state.clonedSpaces.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Clone an app first to toggle individual PIN & Biometric locks per cloned space or Home Screen shortcut.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateInk,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.clonedSpaces, key = { it.id }) { space ->
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
                            packageName = space.packageName,
                            getCachedIcon = getCachedIconBitmap,
                            loadIcon = loadIconBitmap
                        )
                        BadgedAppIcon(
                            displayName = space.displayName,
                            presetKey = space.presetKey,
                            cachedIconBitmap = iconBitmap,
                            badgeText = space.badgeText,
                            badgeColor = Color(space.badgeColorHex),
                            iconSize = 42.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = space.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = JetInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${space.slotLabel} • ${if (space.isLocked) "Protected" else "Open access"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (space.isLocked) AmberDeep else SlateInk
                            )
                        }
                        Switch(
                            checked = space.isLocked,
                            onCheckedChange = { onToggleSpaceLock(space) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = JetInk
                            ),
                            modifier = Modifier.testTag("space_lock_switch_${space.id}")
                        )
                    }
                }
            }
        }
    }
}
