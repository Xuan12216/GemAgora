package com.example.gemagora.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class PinDialogMode {
    SET_NEW,
    CHANGE_EXISTING,
    VERIFY_TO_DISABLE
}

@Composable
fun PinSetupDialog(
    mode: PinDialogMode,
    onDismiss: () -> Unit,
    onVerifyOldPin: suspend (String) -> Boolean,
    onSaveNewPin: suspend (String) -> Unit,
    onDisableLock: suspend () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var step by remember {
        mutableIntStateOf(
            when (mode) {
                PinDialogMode.SET_NEW -> 1
                PinDialogMode.CHANGE_EXISTING -> 0 // Step 0: verify old pin
                PinDialogMode.VERIFY_TO_DISABLE -> 0
            }
        )
    }

    var selectedPinLength by remember { mutableIntStateOf(6) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showPinText by remember { mutableStateOf(false) }

    val currentInput = when (step) {
        0 -> oldPin
        1 -> newPin
        else -> confirmPin
    }

    val maxAllowedLength = if (step == 0) 6 else selectedPinLength

    val stepTitle = when (step) {
        0 -> if (mode == PinDialogMode.VERIFY_TO_DISABLE) "確認解除鎖定" else "驗證目前密碼"
        1 -> if (mode == PinDialogMode.CHANGE_EXISTING) "設定新密碼" else "設定解鎖密碼"
        else -> "再次確認新密碼"
    }

    val stepDesc = when (step) {
        0 -> "請輸入目前使用的 PIN 碼進行身分驗證。"
        1 -> "請選擇密碼長度，並輸入 $selectedPinLength 位數純數字解鎖密碼。"
        else -> "請再次輸入剛剛設定的 $selectedPinLength 位數新密碼。"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stepDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // 密碼長度選擇器 (僅在設定新密碼第一步顯示)
                if (step == 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(4 to "4 位數 PIN 碼", 6 to "6 位數 PIN 碼").forEachIndexed { index, (len, label) ->
                            SegmentedButton(
                                selected = selectedPinLength == len,
                                onClick = {
                                    selectedPinLength = len
                                    if (newPin.length > len) newPin = newPin.take(len)
                                    if (confirmPin.length > len) confirmPin = confirmPin.take(len)
                                    errorMessage = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { input ->
                        if (input.length <= maxAllowedLength && input.all { it.isDigit() }) {
                            errorMessage = null
                            when (step) {
                                0 -> oldPin = input
                                1 -> newPin = input
                                2 -> confirmPin = input
                            }
                        }
                    },
                    label = {
                        Text(
                            if (step == 0) "目前 PIN 碼 (4~6 位數)"
                            else "新 PIN 碼 ($selectedPinLength 位數)"
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPinText = !showPinText }) {
                            Icon(
                                imageVector = if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPinText) "隱藏密碼" else "顯示密碼"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            val isButtonEnabled = when (step) {
                0 -> !isSubmitting && oldPin.length >= 4
                1 -> !isSubmitting && newPin.length == selectedPinLength
                2 -> !isSubmitting && confirmPin.length == selectedPinLength
                else -> false
            }

            Button(
                enabled = isButtonEnabled,
                onClick = {
                    when (step) {
                        0 -> {
                            isSubmitting = true
                            coroutineScope.launch {
                                val isCorrect = onVerifyOldPin(oldPin)
                                isSubmitting = false
                                if (isCorrect) {
                                    if (mode == PinDialogMode.VERIFY_TO_DISABLE) {
                                        onDisableLock()
                                        onDismiss()
                                    } else {
                                        step = 1
                                    }
                                } else {
                                    errorMessage = "目前密碼不正確，請重新輸入"
                                    oldPin = ""
                                }
                            }
                        }
                        1 -> {
                            if (newPin.length != selectedPinLength) {
                                errorMessage = "請輸入完整 $selectedPinLength 位數密碼"
                            } else {
                                step = 2
                            }
                        }
                        2 -> {
                            if (newPin != confirmPin) {
                                errorMessage = "兩次輸入的密碼不相符，請重新輸入"
                                confirmPin = ""
                            } else {
                                isSubmitting = true
                                coroutineScope.launch {
                                    onSaveNewPin(newPin)
                                    isSubmitting = false
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            ) {
                Text(
                    when {
                        step == 0 && mode == PinDialogMode.VERIFY_TO_DISABLE -> "確認解除"
                        step < 2 && mode != PinDialogMode.VERIFY_TO_DISABLE -> "下一步"
                        else -> "完成設定"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
