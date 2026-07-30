package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanProViewModel
import com.example.ui.theme.ThemeMode

@Composable
fun ProfileScreen(
    viewModel: ScanProViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToSetupWizard: (() -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val storageUsed by viewModel.storageUsedBytes.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showPremiumModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    var appLockEnabled by remember { mutableStateOf(false) }
    var cloudBackupEnabled by remember { mutableStateOf(true) }

    val user = currentUser
    val usedMb = (storageUsed ?: 125_000_000L) / (1024 * 1024)
    val totalGb = (user?.storageLimitBytes ?: 5_000_000_000L) / (1024 * 1024 * 1024)
    val storageProgress = (storageUsed ?: 125_000_000L).toFloat() / (user?.storageLimitBytes ?: 5_000_000_000L).toFloat()

    val primaryCyan = MaterialTheme.colorScheme.primary

    // Premium Subscription Modal
    if (showPremiumModal) {
        AlertDialog(
            onDismissRequest = { showPremiumModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ScanPro AI PRO", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Unlock Unlimited Scans, 100 GB Cloud Vault & Full Gemini AI Document Assistant.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, primaryCyan, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pro Yearly Plan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("$29.99 / year (Save 50%)", fontSize = 13.sp, color = primaryCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.authRepository.upgradeToPremium()
                        showPremiumModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Upgrade Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumModal = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Settings Modal
    if (showSettingsModal) {
        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("App Settings & Security", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Lock PIN / Biometrics", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { appLockEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Cloud Backup & Sync", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = cloudBackupEnabled,
                            onCheckedChange = { cloudBackupEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryCyan)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryCyan, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.displayName ?: "Alex Vance",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user?.email ?: "alex.vance@scanpro.ai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, primaryCyan, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("PRO MEMBER • UNLIMITED VAULT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryCyan)
                    }
                }
            }
        }

        // Storage Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cloud Vault Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("$usedMb MB / $totalGb GB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { storageProgress.coerceIn(0.01f, 1.0f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Theme Mode Selector Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Theme Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "App Theme Mode",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                "Dark, Light, or System Default",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            Triple("Dark", ThemeMode.DARK, Icons.Default.DarkMode),
                            Triple("Light", ThemeMode.LIGHT, Icons.Default.LightMode),
                            Triple("Default", ThemeMode.SYSTEM, Icons.Default.SettingsSuggest)
                        )

                        options.forEach { (label, mode, icon) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Menu List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileOptionRow(
                    icon = Icons.Default.Star,
                    title = "ScanPro AI PRO Subscription",
                    subtitle = "Manage plan, billing & limits",
                    color = Color(0xFFF59E0B),
                    onClick = { showPremiumModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.Security,
                    title = "App Lock & Security Settings",
                    subtitle = "PIN lock, biometrics & encryption",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { showSettingsModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Backup & Vault Sync",
                    subtitle = "Firebase cloud sync & backup",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { showSettingsModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.Language,
                    title = "OCR & App Language",
                    subtitle = "Select default scan languages",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { showSettingsModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.Security,
                    title = "App Permissions & Setup Wizard",
                    subtitle = "Re-run First-Time setup & permissions",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.markSetupWizardCompleted(false)
                        onNavigateToSetupWizard?.invoke()
                    }
                )

                ProfileOptionRow(
                    icon = Icons.Default.Logout,
                    title = "Log Out",
                    subtitle = "Sign out of ScanPro AI Vault",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.authRepository.logout()
                        onNavigateToAuth()
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
