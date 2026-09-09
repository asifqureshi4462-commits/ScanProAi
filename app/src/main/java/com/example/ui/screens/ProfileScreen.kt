package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val cloudBackupEnabled by viewModel.cloudBackupEnabled.collectAsState()
    val ocrLanguage by viewModel.ocrLanguage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val context = LocalContext.current

    var showPremiumModal by remember { mutableStateOf(false) }
    var showSecurityModal by remember { mutableStateOf(false) }
    var showCloudModal by remember { mutableStateOf(false) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showAboutModal by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncStatusMessage) {
        syncStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearSyncStatus()
        }
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    val user = currentUser
    val usedMb = (storageUsed ?: 0L) / (1024 * 1024)
    val totalGb = (user?.storageLimitBytes ?: 5_000_000_000L) / (1024 * 1024 * 1024)
    val storageProgress = (storageUsed ?: 0L).toFloat() / (user?.storageLimitBytes ?: 5_000_000_000L).toFloat()

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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "⚠️ Demo mode: no payment is actually processed. This app isn't connected to Google Play Billing yet, so tapping Upgrade just unlocks the premium UI locally for testing.",
                        fontSize = 11.sp,
                        color = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                        viewModel.upgradeToPremiumDemo()
                        showPremiumModal = false
                        Toast.makeText(context, "Demo premium unlocked (no real payment was made)", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Upgrade Now (Demo)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumModal = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // App Lock & Security Modal — real PIN persistence + enforcement
    if (showSecurityModal) {
        AlertDialog(
            onDismissRequest = { showSecurityModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("App Lock & Security", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock PIN", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (viewModel.hasAppLockPin()) "PIN is set — required on app launch" else "No PIN set yet",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (viewModel.hasAppLockPin()) {
                                        viewModel.setAppLockEnabled(true)
                                    } else {
                                        showSetPinDialog = true
                                    }
                                } else {
                                    viewModel.setAppLockEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryCyan)
                        )
                    }
                    if (viewModel.hasAppLockPin()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showSetPinDialog = true }) {
                            Text("Change PIN", color = primaryCyan)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSecurityModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Set/Change PIN Dialog
    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Set App Lock PIN", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6) newPin = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN (4-6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPin.length < 4 -> pinError = "PIN must be at least 4 digits."
                            newPin != confirmPin -> pinError = "PINs don't match."
                            else -> {
                                viewModel.setAppLockPin(newPin)
                                viewModel.setAppLockEnabled(true)
                                showSetPinDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Cloud Backup & Sync Modal — real toggle + real Firestore sync trigger
    if (showCloudModal) {
        AlertDialog(
            onDismissRequest = { showCloudModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Cloud Backup & Vault Sync", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Cloud Backup & Sync", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = cloudBackupEnabled,
                            onCheckedChange = { viewModel.setCloudBackupEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSyncing) "Syncing..." else "Sync Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudModal = false }) {
                    Text("Done", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // OCR & App Language Modal — real picker, persisted and used as the
    // default language in the OCR tool
    if (showLanguageModal) {
        AlertDialog(
            onDismissRequest = { showLanguageModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Default OCR Language", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("English", "Spanish", "French", "German", "Japanese", "Hindi", "Urdu").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setOcrLanguage(lang) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang, color = MaterialTheme.colorScheme.onSurface)
                            if (ocrLanguage == lang) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = primaryCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLanguageModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // About ScanPro AI Modal
    if (showAboutModal) {
        AlertDialog(
            onDismissRequest = { showAboutModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.scanpro_logo),
                            contentDescription = "ScanPro AI Logo",
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ScanPro AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                        Text("v3.2.0 • Build 1042", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ScanPro AI is an all-in-one AI Document Scanner, OCR Engine, PDF Suite, and Cloud Vault powered by Gemini AI.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val pkg = context.packageName
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                                } catch (e: ActivityNotFoundException) {
                                    openUrl("https://play.google.com/store/apps/details?id=$pkg")
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = primaryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rate App on Play Store", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val pkg = context.packageName
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Check out ScanPro AI — an AI document scanner & PDF toolkit: https://play.google.com/store/apps/details?id=$pkg")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share ScanPro AI"))
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Share ScanPro AI with Friends", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@scanpro.ai")
                                    putExtra(Intent.EXTRA_SUBJECT, "ScanPro AI Support Request")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Help Center & Customer Support", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { openUrl("https://www.termsfeed.com/live/00000000-0000-0000-0000-000000000000") }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Privacy Policy & Terms of Service", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutModal = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = primaryCyan)
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
                        text = user?.displayName ?: "Guest User",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user?.email ?: "Not signed in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, primaryCyan, RoundedCornerShape(20.dp))
                            .clickable { if (user?.isPremium != true) showPremiumModal = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (user?.isPremium == true) "PRO MEMBER • UNLIMITED VAULT" else "FREE PLAN • TAP TO UPGRADE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryCyan
                        )
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
                    subtitle = if (appLockEnabled) "PIN lock enabled" else "PIN lock, biometrics & encryption",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { showSecurityModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Backup & Vault Sync",
                    subtitle = if (isSyncing) "Syncing..." else "Firebase cloud sync & backup",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { showCloudModal = true }
                )

                ProfileOptionRow(
                    icon = Icons.Default.Language,
                    title = "OCR & App Language",
                    subtitle = "Default: $ocrLanguage",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { showLanguageModal = true }
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
                    icon = Icons.Default.Info,
                    title = "About ScanPro AI",
                    subtitle = "Version, Rate App, Help & Privacy Policy",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { showAboutModal = true }
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
