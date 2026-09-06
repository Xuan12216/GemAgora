package com.example.gemagora.ui.setup

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.gemagora.data.model.SecuritySettings
import com.example.gemagora.security.BiometricAuthResult
import com.example.gemagora.security.BiometricAvailability
import com.example.gemagora.security.BiometricHelper
import com.example.gemagora.ui.components.SettingsCard
import com.example.gemagora.ui.security.PinDialogMode
import com.example.gemagora.ui.security.PinSetupDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsCard(
    settings: SecuritySettings,
    onVerifyPin: suspend (String) -> Boolean,
    onSavePin: (String) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSetAutoLockTimeout: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var activeDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }
    val biometricAvailability = remember { BiometricHelper.checkBiometricAvailability(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 應用程式密碼鎖定卡片
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "應用程式密碼鎖定",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.isAppLockEnabled) "密碼保護已啟用" else "尚未啟用密碼鎖定",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.isAppLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = settings.isAppLockEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (!settings.hasPin) {
                                activeDialogMode = PinDialogMode.SET_NEW
                            } else {
                                onToggleAppLock(true)
                            }
                        } else {
                            // Require PIN verification to turn off
                            activeDialogMode = PinDialogMode.VERIFY_TO_DISABLE
                        }
                    }
                )
            }

            Text(
                text = "啟用後，每次重新啟動 GemAgora 或於背景閒置逾時返回時，均需輸入安全 PIN 碼解鎖，保護哲學思辨與私密日記。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = settings.isAppLockEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider()

                    // 修改密碼列
                    Surface(
                        onClick = { activeDialogMode = PinDialogMode.CHANGE_EXISTING },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Password,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "變更解鎖密碼",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 自動鎖定時機
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自動鎖定時機",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = settings.autoLockTimeoutLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SecuritySettings.TIMEOUT_OPTIONS.forEachIndexed { index, (seconds, label) ->
                                SegmentedButton(
                                    selected = settings.autoLockTimeoutSeconds == seconds,
                                    onClick = { onSetAutoLockTimeout(seconds) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = SecuritySettings.TIMEOUT_OPTIONS.size
                                    )
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 指紋與生物辨識卡片
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (settings.isBiometricEnabled) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (settings.isBiometricEnabled) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "指紋 / 生物辨識解鎖",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.isBiometricEnabled) "已啟用快速感應解鎖" else "未啟用生物辨識",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = settings.isBiometricEnabled,
                    enabled = settings.isAppLockEnabled && biometricAvailability.isAvailable,
                    onCheckedChange = { isChecked ->
                        if (isChecked && activity != null) {
                            // Test biometric immediately when turning on
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = "驗證指紋以啟用",
                                subtitle = "請感應指紋以啟用快速解鎖",
                                negativeButtonText = "取消"
                            ) { result ->
                                if (result is BiometricAuthResult.Success) {
                                    onToggleBiometric(true)
                                    Toast.makeText(context, "指紋辨識已成功啟用！", Toast.LENGTH_SHORT).show()
                                } else if (result is BiometricAuthResult.Failed) {
                                    Toast.makeText(context, "辨識失敗，請重試", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            onToggleBiometric(false)
                        }
                    }
                )
            }

            // 硬體狀態說明標籤
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (biometricAvailability.isAvailable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (biometricAvailability.isAvailable) Color(0xFF10B981)
                                else MaterialTheme.colorScheme.error
                            )
                    )
                    Text(
                        text = "裝置支援狀態：${biometricAvailability.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!settings.isAppLockEnabled) {
                Text(
                    text = "提示：需先啟用上方「應用程式密碼鎖定」，方可開啟指紋快速解鎖。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else if (settings.isBiometricEnabled && activity != null) {
                OutlinedButton(
                    onClick = {
                        BiometricHelper.authenticate(
                            activity = activity,
                            title = "測試指紋辨識",
                            subtitle = "請感應指紋測試解鎖流程",
                            negativeButtonText = "關閉"
                        ) { result ->
                            when (result) {
                                is BiometricAuthResult.Success -> {
                                    Toast.makeText(context, "指紋測試成功！驗證通過", Toast.LENGTH_SHORT).show()
                                }
                                is BiometricAuthResult.Failed -> {
                                    Toast.makeText(context, "指紋測試：特徵不符", Toast.LENGTH_SHORT).show()
                                }
                                is BiometricAuthResult.Error -> {
                                    Toast.makeText(context, "指紋測試錯誤：${result.errString}", Toast.LENGTH_SHORT).show()
                                }
                                BiometricAuthResult.UserCanceled -> {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("立即測試指紋辨識功能")
                }
            }
        }

        // 3. 哲學隱私保障承諾卡片
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "100% 地端離線哲學隱私承諾",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "GemAgora 秉持古希臘雅典學園的思辨自由精神。您的所有對話、反詰提問紀錄與思考日記，均保存在本地端 SQLite / Room 資料庫中，完全不經由任何雲端伺服器傳輸。" +
                        "\n\n搭配本機密碼鎖定與指紋辨識，讓您的靈魂沉思與內省只屬於您一人。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }

    // 密碼設定/變更/解除對話框
    activeDialogMode?.let { mode ->
        PinSetupDialog(
            mode = mode,
            onDismiss = { activeDialogMode = null },
            onVerifyOldPin = onVerifyPin,
            onSaveNewPin = { newPin ->
                onSavePin(newPin)
                Toast.makeText(context, "密碼已更新並啟用鎖定！", Toast.LENGTH_SHORT).show()
            },
            onDisableLock = {
                onToggleAppLock(false)
                Toast.makeText(context, "已關閉應用程式鎖定", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
