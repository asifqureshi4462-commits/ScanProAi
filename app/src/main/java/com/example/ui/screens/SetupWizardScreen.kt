package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

data class PermissionStep(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val privacyNote: String,
    val icon: ImageVector,
    val androidPermission: String? = null,
    val isMandatory: Boolean = false,
    val isInformationalOnly: Boolean = false,
    val isSettingsIntent: Boolean = false
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current

    // Build steps list
    val steps = remember {
        listOf(
            PermissionStep(
                id = "welcome",
                title = "Welcome to ScanPro AI",
                subtitle = "First-Time App Setup",
                description = "ScanPro AI uses hardware components and system integrations to deliver real-time OpenCV document edge detection, AI summaries, and cloud document sync.",
                privacyNote = "Your privacy is guaranteed. All documents are stored securely with local AES-256 encryption.",
                icon = Icons.Default.RocketLaunch,
                isInformationalOnly = true
            ),
            PermissionStep(
                id = "camera",
                title = "Camera Access",
                subtitle = "Document Scanning & Auto-Crop",
                description = "Required to capture document pages and automatically highlight/crop document edges in real-time using OpenCV.",
                privacyNote = "The camera stream is processed entirely on-device and is active only while scanning.",
                icon = Icons.Default.CameraAlt,
                androidPermission = Manifest.permission.CAMERA,
                isMandatory = true
            ),
            PermissionStep(
                id = "storage",
                title = "Photos & Media Access",
                subtitle = "File Import & Gallery Sync",
                description = "Allows importing existing PDF files or document photos directly from your device storage into ScanPro AI.",
                privacyNote = "ScanPro AI strictly accesses files you explicitly pick or export.",
                icon = Icons.Default.PermMedia,
                androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ),
            PermissionStep(
                id = "notifications",
                title = "System Notifications",
                subtitle = "OCR & Export Status",
                description = "Receive instant alerts when batch OCR extraction completes or when cloud PDF backups finish in the background.",
                privacyNote = "Only essential job completion alerts are sent. No promotional spam.",
                icon = Icons.Default.NotificationsActive,
                androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null,
                isInformationalOnly = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ),
            PermissionStep(
                id = "microphone",
                title = "Microphone Access",
                subtitle = "Voice Notes & Dictation",
                description = "Powers hands-free AI voice dictation and audio summary features when talking to the ScanPro AI assistant.",
                privacyNote = "Microphone recording is triggered strictly when you tap the voice dictation button.",
                icon = Icons.Default.Mic,
                androidPermission = Manifest.permission.RECORD_AUDIO
            ),
            PermissionStep(
                id = "location",
                title = "Location Tagging",
                subtitle = "Optional Geotagged Scans",
                description = "Automatically tags scans with location metadata, allowing you to organize and search documents by scan location.",
                privacyNote = "Location coordinates remain stored locally in document metadata.",
                icon = Icons.Default.LocationOn,
                androidPermission = Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionStep(
                id = "manage_files",
                title = "Full File System Storage",
                subtitle = "Direct PDF Export to Downloads",
                description = "Enables saving merged high-resolution PDF books directly into your system Download folders for easy sharing.",
                privacyNote = "Managed via standard Android Storage Access Framework.",
                icon = Icons.Default.FolderSpecial,
                isSettingsIntent = true
            ),
            PermissionStep(
                id = "battery",
                title = "Battery Optimization",
                subtitle = "Uninterrupted Batch Exports",
                description = "Exempts ScanPro AI from aggressive background killing during long multi-page PDF rendering and OCR jobs.",
                privacyNote = "ScanPro AI uses background CPU only while active PDF jobs are running.",
                icon = Icons.Default.BatterySaver,
                isSettingsIntent = true
            ),
            PermissionStep(
                id = "internet",
                title = "Internet & Cloud Services",
                subtitle = "AI Summaries & Translation",
                description = "Used to connect to Google Gemini AI models for document summarization, language translation, and cloud sync.",
                privacyNote = "All transmission is secured over TLS 1.3 encrypted sockets.",
                icon = Icons.Default.Wifi,
                isInformationalOnly = true
            ),
            PermissionStep(
                id = "accessibility",
                title = "Accessibility & Voice Control",
                subtitle = "Screen Reader & Assistive Mode",
                description = "Fully compatible with TalkBack screen readers, high-contrast UI, and assistive touch controls.",
                privacyNote = "Designed according to Android Accessibility standards.",
                icon = Icons.Default.AccessibilityNew,
                isInformationalOnly = true
            ),
            PermissionStep(
                id = "completion",
                title = "Setup Complete!",
                subtitle = "ScanPro AI is Ready",
                description = "All permissions and system parameters have been configured. You are ready to start scanning and analyzing documents with AI.",
                privacyNote = "You can modify your permissions anytime from system Settings -> Apps -> ScanPro AI.",
                icon = Icons.Default.CheckCircle,
                isInformationalOnly = true
            )
        )
    }

    var currentStepIndex by remember { mutableStateOf(0) }
    val grantedMap = remember { mutableStateMapOf<String, Boolean>() }
    var deniedMessage by remember { mutableStateOf<String?>(null) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    val currentStep = steps[currentStepIndex]
    val totalSteps = steps.size
    val progressFloat by animateFloatAsState(
        targetValue = (currentStepIndex + 1).toFloat() / totalSteps.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    // Check existing permissions on launch
    LaunchedEffect(Unit) {
        steps.forEach { step ->
            if (step.androidPermission != null) {
                val status = ContextCompat.checkSelfPermission(context, step.androidPermission)
                grantedMap[step.id] = (status == PackageManager.PERMISSION_GRANTED)
            } else if (step.isInformationalOnly || step.id == "welcome" || step.id == "completion") {
                grantedMap[step.id] = true
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        grantedMap[currentStep.id] = isGranted
        if (isGranted) {
            deniedMessage = null
            if (currentStepIndex < totalSteps - 1) {
                currentStepIndex++
            }
        } else {
            deniedMessage = "${currentStep.title} was denied. Some features will be restricted."
            showDeniedDialog = true
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Deep Blue
            Color(0xFF0284C7), // Sky Blue / Cyan
            Color(0xFF7C3AED)  // Vibrant Purple
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(gradientBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SCANPRO SETUP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "STEP ${currentStepIndex + 1} OF $totalSteps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progressFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Card Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { width -> width },
                            animationSpec = tween(350)
                        ) + fadeIn(animationSpec = tween(350)) with
                                slideOutHorizontally(
                                    targetOffsetX = { width -> -width },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                    }
                ) { targetStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glassmorphic Card Container
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.outline
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Animated Icon Circle Badge
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = targetStep.icon,
                                        contentDescription = targetStep.title,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = targetStep.subtitle.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = targetStep.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = targetStep.description,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Privacy Assurance Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.PrivacyTip,
                                            contentDescription = "Privacy",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "PRIVACY ASSURANCE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = targetStep.privacyNote,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }

                                // Granted Status Badge if already granted
                                if (grantedMap[targetStep.id] == true && !targetStep.isInformationalOnly) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Permission Granted",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Section
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentStep.id == "completion") {
                    Button(
                        onClick = { onSetupComplete() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LAUNCH HOME DASHBOARD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                } else if (currentStep.id == "welcome") {
                    Button(
                        onClick = { currentStepIndex++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "START SETUP WIZARD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                } else {
                    // Main Action Button
                    Button(
                        onClick = {
                            val perm = currentStep.androidPermission
                            if (perm != null) {
                                val isAlready = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                                if (isAlready) {
                                    grantedMap[currentStep.id] = true
                                    if (currentStepIndex < totalSteps - 1) currentStepIndex++
                                } else {
                                    permissionLauncher.launch(perm)
                                }
                            } else if (currentStep.isSettingsIntent) {
                                openAppSettings()
                                grantedMap[currentStep.id] = true
                                if (currentStepIndex < totalSteps - 1) currentStepIndex++
                            } else {
                                grantedMap[currentStep.id] = true
                                if (currentStepIndex < totalSteps - 1) currentStepIndex++
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        val isGranted = grantedMap[currentStep.id] == true
                        val btnText = when {
                            isGranted -> "CONTINUE NEXT"
                            currentStep.isSettingsIntent -> "OPEN SYSTEM SETTINGS"
                            currentStep.isInformationalOnly -> "ACKNOWLEDGE & CONTINUE"
                            else -> "ALLOW PERMISSION"
                        }
                        Text(
                            text = btnText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action: Skip or Previous
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", fontSize = 13.sp)
                            }
                        }

                        if (!currentStep.isMandatory && currentStepIndex < totalSteps - 1) {
                            if (currentStepIndex > 0) Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    if (currentStepIndex < totalSteps - 1) currentStepIndex++
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Skip for Now", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Denied Friendly Dialog
    if (showDeniedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeniedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permission Required", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = deniedMessage ?: "Permission was denied. You can proceed, but features requiring this permission may be disabled.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeniedDialog = false
                        openAppSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeniedDialog = false
                        if (currentStepIndex < totalSteps - 1) currentStepIndex++
                    }
                ) {
                    Text("Continue Anyway")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
