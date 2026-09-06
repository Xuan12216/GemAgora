package com.example.gemagora.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onVerifyPin: suspend (String) -> Boolean,
    onUnlockSuccess: () -> Unit,
    isBiometricEnabled: Boolean,
    onBiometricRequest: () -> Unit,
    pinLength: Int = 4,
    modifier: Modifier = Modifier
) {
    val targetLength = pinLength.coerceIn(4, 6)
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    // Auto-trigger biometric on first launch if enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            delay(300)
            onBiometricRequest()
        }
    }

    fun submitPin(pin: String) {
        if (isVerifying) return
        isVerifying = true
        coroutineScope.launch {
            val isValid = onVerifyPin(pin)
            if (isValid) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlockSuccess()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                errorMessage = "密碼錯誤，請重新輸入"
                enteredPin = ""
                // Shake animation
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        -25f at 50 using FastOutSlowInEasing
                        25f at 100 using FastOutSlowInEasing
                        -20f at 150 using FastOutSlowInEasing
                        20f at 200 using FastOutSlowInEasing
                        -10f at 250 using FastOutSlowInEasing
                        10f at 300 using FastOutSlowInEasing
                        0f at 400
                    }
                )
            }
            isVerifying = false
        }
    }

    fun onKeyPressed(digit: String) {
        if (isVerifying) return
        errorMessage = null
        if (enteredPin.length < targetLength) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == targetLength) {
                submitPin(newPin)
            } else if (newPin.length >= 4 && targetLength > 4) {
                coroutineScope.launch {
                    val isValid = onVerifyPin(newPin)
                    if (isValid) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnlockSuccess()
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (isVerifying) return
        errorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))

                // Top Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Lock",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "GemAgora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "隱私鎖定保護中",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "請輸入解鎖密碼，守護您的思辨與對話紀錄",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Center PIN Indicator Dots
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    val totalDots = targetLength
                    val dotSpacing = if (totalDots > 4) 12.dp else 16.dp
                    val dotBaseSize = if (totalDots > 4) 12.dp else 14.dp

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalDots) {
                            val isFilled = i < enteredPin.length
                            val dotScale by animateFloatAsState(
                                targetValue = if (isFilled) 1.25f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "dotScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size((dotBaseSize.value * dotScale).dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Numeric Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("bio", "0", "del")
                    )

                    keys.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "bio" -> {
                                        if (isBiometricEnabled) {
                                            IconButton(
                                                onClick = onBiometricRequest,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "指紋解鎖",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.size(72.dp))
                                        }
                                    }
                                    "del" -> {
                                        IconButton(
                                            onClick = { onBackspace() },
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "刪除",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        KeypadButton(
                                            digit = key,
                                            onClick = { onKeyPressed(key) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(72.dp),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
