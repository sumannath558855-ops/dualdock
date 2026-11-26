package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CanvasWhite
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderTint
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VaultLockScreen(
    title: String,
    subtitle: String,
    pinLength: Int,
    canUseBiometric: Boolean,
    errorText: String?,
    onVerifyPin: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorText) {
        if (!errorText.isNullOrEmpty()) {
            enteredPin = ""
            scope.launch {
                listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, animationSpec = tween(42))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 390.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Clean Pastel Lavender Shield Emblem
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(LavenderTint),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(PastelLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.cd_clone_locked),
                        tint = JetInk,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = JetInk,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Crisp Jet-Black PIN Dot Indicators
            Row(
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pinLength) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !errorText.isNullOrEmpty() && !filled -> DangerCoral.copy(alpha = 0.25f)
                                    filled -> JetInk
                                    else -> PureWhite
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (filled) JetInk else HairlineBorder,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.height(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!errorText.isNullOrEmpty()) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerCoral,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            LightPinKey(
                                digit = digit,
                                onClick = {
                                    if (enteredPin.length < pinLength) {
                                        val updated = enteredPin + digit
                                        enteredPin = updated
                                        if (updated.length == pinLength) {
                                            onVerifyPin(updated)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canUseBiometric) {
                        Surface(
                            shape = CircleShape,
                            color = PastelLavender,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onBiometricClick)
                                .testTag("biometric_unlock_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = stringResource(R.string.cd_biometric_unlock),
                                    tint = JetInk,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    LightPinKey(
                        digit = "0",
                        onClick = {
                            if (enteredPin.length < pinLength) {
                                val updated = enteredPin + "0"
                                enteredPin = updated
                                if (updated.length == pinLength) {
                                    onVerifyPin(updated)
                                }
                            }
                        }
                    )

                    Surface(
                        shape = CircleShape,
                        color = PureWhite,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            }
                            .testTag("pin_backspace_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = stringResource(R.string.cd_backspace),
                                tint = SlateInk,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("lock_cancel_button")
                ) {
                    Text(text = "Cancel", color = SlateInk)
                }
            }
        }
    }
}

@Composable
private fun LightPinKey(
    digit: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = PureWhite,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$digit")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = JetInk
            )
        }
    }
}
