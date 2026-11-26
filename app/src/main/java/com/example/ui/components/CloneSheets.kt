package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloneEngineType
import com.example.data.ClonedSpace
import com.example.ui.CloneConfigDraft
import com.example.ui.theme.AmberDeep
import com.example.ui.theme.BadgeSwatches
import com.example.ui.theme.CanvasWhite
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.JetInk
import com.example.ui.theme.LavenderTint
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PinkDeep
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SoftCloud

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CloneConfigBottomSheet(
    draft: CloneConfigDraft,
    appIconDrawable: Drawable? = null,
    cachedIconBitmap: ImageBitmap? = null,
    canPinShortcut: Boolean,
    onDismiss: () -> Unit,
    onSave: (CloneConfigDraft) -> Unit,
    onUseNativeWorkMode: ((String, String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(draft) { mutableStateOf(draft.displayName) }
    var targetUrl by remember(draft) { mutableStateOf(draft.targetUrl) }
    var badgeText by remember(draft) { mutableStateOf(draft.badgeText) }
    var badgeColorHex by remember(draft) { mutableStateOf(draft.badgeColorHex) }
    var useDesktopUa by remember(draft) { mutableStateOf(draft.useDesktopUserAgent) }
    var isLocked by remember(draft) { mutableStateOf(draft.isLocked) }
    var pinShortcut by remember(draft) { mutableStateOf(draft.pinShortcutOnSave) }
    var batchCount by remember(draft) { mutableIntStateOf(draft.batchCount) }

    val badgeSuggestions = listOf("2", "3", "4", "W", "B", "P")
    val batchOptions = listOf(1, 2, 3, 5)
    val selectedColor = Color(badgeColorHex)
    val selectedBadgeInk = if (selectedColor.luminance() > 0.55f) JetInk else PureWhite

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        contentColor = JetInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BadgedAppIcon(
                    displayName = name.ifBlank { "Clone" },
                    presetKey = draft.presetKey,
                    appIconDrawable = appIconDrawable,
                    cachedIconBitmap = cachedIconBitmap,
                    badgeText = badgeText.ifBlank { "2" },
                    badgeColor = selectedColor,
                    iconSize = 58.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (draft.editingId == null) "Create Cloned Space" else "Edit Cloned Space",
                        style = MaterialTheme.typography.titleLarge,
                        color = JetInk
                    )
                    Text(
                        text = if (draft.engineType == CloneEngineType.WORK_PROFILE)
                            "Native Android Work Profile instance"
                        else "Unlimited isolated multi-account container",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepLavender
                    )
                }
            }

            if (!draft.helperHint.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LavenderTint
                ) {
                    Text(
                        text = draft.helperHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = JetInk.copy(alpha = 0.85f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (draft.editingId == null &&
                draft.engineType == CloneEngineType.WEB_CONTAINER &&
                draft.packageName.isNotBlank() &&
                onUseNativeWorkMode != null
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelMint.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onUseNativeWorkMode(
                                name.ifBlank { "App" },
                                draft.packageName
                            )
                        }
                        .testTag("switch_to_native_work_mode_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Want 'Get OTP on WhatsApp'?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = JetInk
                            )
                            Text(
                                text = "Switch to Native Work Mode to run the full Play Store app with WhatsApp OTP & auto-fill.",
                                style = MaterialTheme.typography.bodySmall,
                                color = JetInk.copy(alpha = 0.78f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = JetInk
                        ) {
                            Text(
                                text = "Native →",
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (draft.editingId == null && draft.engineType == CloneEngineType.WEB_CONTAINER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AllInclusive,
                            contentDescription = null,
                            tint = DeepLavender,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BATCH CLONE COUNT (UNLIMITED)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateInk
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        batchOptions.forEach { count ->
                            val selected = batchCount == count
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) JetInk else SoftCloud,
                                border = if (selected) null else BorderStroke(1.dp, HairlineBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { batchCount = count }
                                    .testTag("batch_count_$count")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (count == 1) "1 Clone" else "${count}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) PureWhite else JetInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Clone Name") },
                placeholder = { Text("e.g., Flipkart — Account 2") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clone_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JetInk,
                    unfocusedBorderColor = HairlineBorder,
                    focusedLabelColor = JetInk
                )
            )

            if (draft.engineType == CloneEngineType.WEB_CONTAINER) {
                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text("App Web URL") },
                    placeholder = { Text("https://www.flipkart.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clone_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JetInk,
                        unfocusedBorderColor = HairlineBorder,
                        focusedLabelColor = JetInk
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BADGE MARKER & COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateInk
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badgeSuggestions.forEach { presetBadge ->
                        val selected = badgeText == presetBadge
                        Surface(
                            shape = CircleShape,
                            color = if (selected) selectedColor else SoftCloud,
                            border = if (selected) null else BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { badgeText = presetBadge }
                                .testTag("badge_preset_$presetBadge")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = presetBadge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) selectedBadgeInk else JetInk
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it.take(2).uppercase() },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("2") },
                        modifier = Modifier
                            .width(68.dp)
                            .testTag("custom_badge_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JetInk,
                            unfocusedBorderColor = HairlineBorder
                        )
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BadgeSwatches.forEach { (label, colorHex) ->
                        val selected = badgeColorHex == colorHex
                        val swatchColor = Color(colorHex)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) JetInk else HairlineBorder,
                                    shape = CircleShape
                                )
                                .clickable { badgeColorHex = colorHex }
                                .testTag("badge_color_$label"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = label,
                                    tint = if (swatchColor.luminance() > 0.55f) JetInk else PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CanvasWhite,
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    if (draft.engineType == CloneEngineType.WEB_CONTAINER) {
                        SheetToggleRow(
                            title = "Desktop Container Mode",
                            subtitle = "Required for WhatsApp Web QR linking",
                            checked = useDesktopUa,
                            onCheckedChange = { useDesktopUa = it },
                            tag = "toggle_desktop_ua"
                        )
                        HorizontalDivider(color = HairlineBorder)
                    }

                    SheetToggleRow(
                        title = "Require Privacy Lock",
                        subtitle = "Ask for Vault PIN or biometrics before opening",
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        tag = "toggle_clone_lock"
                    )

                    if (canPinShortcut) {
                        HorizontalDivider(color = HairlineBorder)
                        SheetToggleRow(
                            title = "Pin Home Screen Shortcut",
                            subtitle = "Add badged icon directly to your launcher",
                            checked = pinShortcut,
                            onCheckedChange = { pinShortcut = it },
                            tag = "toggle_pin_shortcut"
                        )
                    }
                }
            }

            BentoPrimaryButton(
                text = when {
                    draft.editingId != null -> "Save Changes"
                    batchCount > 1 -> "Create $batchCount Isolated Clones"
                    else -> "Create Cloned Space"
                },
                onClick = {
                    onSave(
                        draft.copy(
                            displayName = name,
                            targetUrl = targetUrl,
                            badgeText = badgeText,
                            badgeColorHex = badgeColorHex,
                            useDesktopUserAgent = useDesktopUa,
                            isLocked = isLocked,
                            pinShortcutOnSave = pinShortcut,
                            batchCount = batchCount
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_clone_button"
            )
        }
    }
}

@Composable
private fun SheetToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = JetInk)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateInk
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = JetInk
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneActionsBottomSheet(
    space: ClonedSpace,
    appIconDrawable: Drawable? = null,
    cachedIconBitmap: ImageBitmap? = null,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onDuplicate: () -> Unit,
    onPinShortcut: () -> Unit,
    onEdit: () -> Unit,
    onToggleLock: () -> Unit,
    onClearSession: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        contentColor = JetInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                BadgedAppIcon(
                    displayName = space.displayName,
                    presetKey = space.presetKey,
                    appIconDrawable = appIconDrawable,
                    cachedIconBitmap = cachedIconBitmap,
                    badgeText = space.badgeText,
                    badgeColor = Color(space.badgeColorHex),
                    iconSize = 52.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = space.displayName, style = MaterialTheme.typography.titleLarge, color = JetInk)
                    Text(
                        text = "${space.slotLabel} • ${if (space.isContainer) space.targetUrl else space.packageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateInk,
                        maxLines = 1
                    )
                }
            }

            ActionMenuRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                label = stringResource(R.string.action_launch),
                tint = DeepLavender,
                tag = "action_launch_clone",
                onClick = onLaunch
            )
            if (space.isContainer) {
                ActionMenuRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Duplicate (+1 New Clone)",
                    tint = PinkDeep,
                    tag = "action_duplicate_clone",
                    onClick = onDuplicate
                )
            }
            ActionMenuRow(
                icon = Icons.Rounded.AddToHomeScreen,
                label = stringResource(R.string.action_pin_shortcut),
                tint = JetInk,
                tag = "action_pin_shortcut",
                onClick = onPinShortcut
            )
            ActionMenuRow(
                icon = Icons.Rounded.Edit,
                label = stringResource(R.string.action_edit),
                tint = JetInk,
                tag = "action_edit_clone",
                onClick = onEdit
            )
            ActionMenuRow(
                icon = if (space.isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                label = if (space.isLocked) "Unlock This Clone" else "Lock With PIN / Biometric",
                tint = AmberDeep,
                tag = "action_toggle_lock_clone",
                onClick = onToggleLock
            )
            if (space.isContainer) {
                ActionMenuRow(
                    icon = Icons.Rounded.DeleteSweep,
                    label = stringResource(R.string.action_clear_session),
                    tint = SlateInk,
                    tag = "action_clear_session",
                    onClick = onClearSession
                )
            }
            HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 4.dp))
            ActionMenuRow(
                icon = Icons.Rounded.DeleteOutline,
                label = stringResource(R.string.action_delete),
                tint = DangerCoral,
                tag = "action_delete_clone",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ActionMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CanvasWhite,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = tint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupBottomSheet(
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pinLength by remember { mutableIntStateOf(4) }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        contentColor = JetInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (firstPin == null) "Create Vault PIN" else "Confirm Vault PIN",
                style = MaterialTheme.typography.headlineMedium,
                color = JetInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (firstPin == null) "Choose a $pinLength-digit PIN to lock your cloned spaces"
                else "Re-enter your $pinLength-digit PIN to confirm",
                style = MaterialTheme.typography.bodySmall,
                color = SlateInk
            )

            if (firstPin == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(4, 6).forEach { lengthOption ->
                        val selected = pinLength == lengthOption
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) JetInk else SoftCloud,
                            border = if (selected) null else BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    pinLength = lengthOption
                                    currentInput = ""
                                    errorMessage = null
                                }
                                .testTag("pin_length_$lengthOption")
                        ) {
                            Text(
                                text = "$lengthOption Digits",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) PureWhite else JetInk,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(pinLength) { idx ->
                    val filled = idx < currentInput.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (filled) JetInk else PureWhite)
                            .border(1.5.dp, if (filled) JetInk else HairlineBorder, CircleShape)
                    )
                }
            }

            Box(
                modifier = Modifier.height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerCoral
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val handleDigit: (String) -> Unit = { digit ->
                errorMessage = null
                if (currentInput.length < pinLength) {
                    val next = currentInput + digit
                    currentInput = next
                    if (next.length == pinLength) {
                        if (firstPin == null) {
                            firstPin = next
                            currentInput = ""
                        } else if (firstPin == next) {
                            onPinConfirmed(next)
                        } else {
                            errorMessage = "PINs did not match. Start over."
                            firstPin = null
                            currentInput = ""
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { d ->
                            SetupKeyButton(text = d, onClick = { handleDigit(d) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(modifier = Modifier.size(64.dp))
                    SetupKeyButton(text = "0", onClick = { handleDigit("0") })
                    Surface(
                        shape = CircleShape,
                        color = CanvasWhite,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
                            }
                            .testTag("setup_pin_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = stringResource(R.string.cd_backspace),
                                tint = SlateInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupKeyButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = CanvasWhite,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("setup_pin_key_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = JetInk
            )
        }
    }
}
