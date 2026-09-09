package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ScannedDocument
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DocumentPreviewScreen
import com.example.ui.screens.EpubZipScreen
import com.example.ui.screens.ExcelEditorScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageEditorScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PdfViewerEditorScreen
import com.example.ui.screens.PresentationViewerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerCameraScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TextCodeEditorScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.WordEditorScreen
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppDestination {
    SPLASH,
    ONBOARDING,
    AUTH,
    SETUP_WIZARD,
    MAIN_TABS,
    SCANNER_CAMERA,
    AI_CHAT,
    PDF_EDITOR,
    WORD_EDITOR,
    EXCEL_EDITOR,
    PRESENTATION_VIEWER,
    IMAGE_EDITOR,
    TEXT_CODE_EDITOR,
    EPUB_READER,
    ZIP_MANAGER,
    DOC_PREVIEW
}

@Composable
fun MainScreen(viewModel: ScanProViewModel = viewModel()) {
    val context = LocalContext.current
    val isSetupCompleted by viewModel.isSetupWizardCompleted.collectAsState()

    var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Files, 2: Scan, 3: Tools, 4: Profile

    // Real App Lock enforcement: re-locks whenever the app is backgrounded,
    // and requires the user's real PIN (set in Profile) to get back in.
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    var appLockPassed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                appLockPassed = false
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    // Central BackHandler: Handles system hardware/gesture back button safely without closing app
    BackHandler(
        enabled = currentDestination != AppDestination.MAIN_TABS || (currentDestination == AppDestination.MAIN_TABS && selectedTab != 0)
    ) {
        if (currentDestination != AppDestination.MAIN_TABS) {
            currentDestination = AppDestination.MAIN_TABS
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    // File Import Launcher
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val docsDir = File(context.filesDir, "imported")
                if (!docsDir.exists()) docsDir.mkdirs()
                val destFile = File(docsDir, "Imported_${System.currentTimeMillis()}.pdf")
                FileOutputStream(destFile).use { out ->
                    inputStream?.copyTo(out)
                }

                val doc = ScannedDocument(
                    title = destFile.name,
                    filePath = destFile.absolutePath,
                    fileType = "PDF",
                    fileSizeBytes = destFile.length(),
                    pageCount = 1,
                    category = "Documents",
                    createdAt = System.currentTimeMillis()
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val db = com.example.data.local.ScanProDatabase.getDatabase(context)
                    val id = db.documentDao().insertDocument(doc)
                    viewModel.setActiveDocument(doc.copy(id = id))
                }
                Toast.makeText(context, "File imported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val openDocumentDedicated: (ScannedDocument) -> Unit = { doc ->
        viewModel.setActiveDocument(doc)
        val ext = doc.fileType.uppercase()
        currentDestination = when {
            ext.contains("PDF") -> AppDestination.PDF_EDITOR
            ext.contains("DOC") || ext.contains("TXT") || ext.contains("RTF") -> AppDestination.WORD_EDITOR
            ext.contains("XLS") || ext.contains("CSV") -> AppDestination.EXCEL_EDITOR
            ext.contains("PPT") -> AppDestination.PRESENTATION_VIEWER
            ext.contains("JPG") || ext.contains("PNG") || ext.contains("WEBP") || ext.contains("JPEG") -> AppDestination.IMAGE_EDITOR
            ext.contains("JSON") || ext.contains("HTML") || ext.contains("XML") -> AppDestination.TEXT_CODE_EDITOR
            ext.contains("EPUB") -> AppDestination.EPUB_READER
            ext.contains("ZIP") -> AppDestination.ZIP_MANAGER
            else -> AppDestination.DOC_PREVIEW
        }
    }

    when (currentDestination) {
        AppDestination.SPLASH -> {
            SplashScreen(
                onSplashFinished = {
                    currentDestination = AppDestination.ONBOARDING
                }
            )
        }

        AppDestination.ONBOARDING -> {
            OnboardingScreen(
                onFinishOnboarding = {
                    currentDestination = AppDestination.AUTH
                }
            )
        }

        AppDestination.AUTH -> {
            AuthScreen(
                authRepository = viewModel.authRepository,
                onAuthSuccess = {
                    if (!isSetupCompleted) {
                        currentDestination = AppDestination.SETUP_WIZARD
                    } else {
                        currentDestination = AppDestination.MAIN_TABS
                    }
                }
            )
        }

        AppDestination.SETUP_WIZARD -> {
            SetupWizardScreen(
                onSetupComplete = {
                    viewModel.markSetupWizardCompleted(true)
                    currentDestination = AppDestination.MAIN_TABS
                }
            )
        }

        AppDestination.SCANNER_CAMERA -> {
            ScannerCameraScreen(
                viewModel = viewModel,
                onCloseScanner = { currentDestination = AppDestination.MAIN_TABS },
                onSaveSuccess = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.AI_CHAT -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    BottomNavWithCenterFab(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            currentDestination = AppDestination.MAIN_TABS
                        },
                        onCenterScanClicked = { currentDestination = AppDestination.SCANNER_CAMERA }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                        .statusBarsPadding()
                ) {
                    AiChatScreen(viewModel = viewModel)
                }
            }
        }

        AppDestination.PDF_EDITOR -> {
            PdfViewerEditorScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.WORD_EDITOR -> {
            WordEditorScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.EXCEL_EDITOR -> {
            ExcelEditorScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.PRESENTATION_VIEWER -> {
            PresentationViewerScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.IMAGE_EDITOR -> {
            ImageEditorScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.TEXT_CODE_EDITOR -> {
            TextCodeEditorScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.EPUB_READER, AppDestination.ZIP_MANAGER -> {
            EpubZipScreen(
                viewModel = viewModel,
                onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
            )
        }

        AppDestination.DOC_PREVIEW -> {
            Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    DocumentPreviewScreen(
                        viewModel = viewModel,
                        onBackClicked = { currentDestination = AppDestination.MAIN_TABS }
                    )
                }
            }
        }

        AppDestination.MAIN_TABS -> {
            if (appLockEnabled && !appLockPassed) {
                AppLockScreen(
                    onUnlock = { pin -> viewModel.verifyAppLockPin(pin) },
                    onUnlocked = { appLockPassed = true }
                )
            } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    BottomNavWithCenterFab(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onCenterScanClicked = { currentDestination = AppDestination.SCANNER_CAMERA }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .statusBarsPadding()
                ) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { currentDestination = AppDestination.SCANNER_CAMERA },
                            onNavigateToAiChat = { currentDestination = AppDestination.AI_CHAT },
                            onNavigateToDocPreview = openDocumentDedicated,
                            onNavigateToTools = { selectedTab = 3 },
                            onImportFileClicked = { fileImportLauncher.launch("application/*") },
                            onNavigateToProfile = { selectedTab = 4 }
                        )

                        1 -> FileManagerScreen(
                            viewModel = viewModel,
                            onOpenDocument = openDocumentDedicated
                        )

                        3 -> ToolsScreen(
                            viewModel = viewModel,
                            onNavigateToDocPreview = openDocumentDedicated,
                            onNavigateToCamera = { currentDestination = AppDestination.SCANNER_CAMERA }
                        )

                        4 -> ProfileScreen(
                            viewModel = viewModel,
                            onNavigateToAuth = { currentDestination = AppDestination.AUTH },
                            onNavigateToSetupWizard = { currentDestination = AppDestination.SETUP_WIZARD }
                        )

                        else -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { currentDestination = AppDestination.SCANNER_CAMERA },
                            onNavigateToAiChat = { currentDestination = AppDestination.AI_CHAT },
                            onNavigateToDocPreview = openDocumentDedicated,
                            onNavigateToTools = { selectedTab = 3 },
                            onImportFileClicked = { fileImportLauncher.launch("application/*") },
                            onNavigateToProfile = { selectedTab = 4 }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun BottomNavWithCenterFab(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCenterScanClicked: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            val indicatorColor = MaterialTheme.colorScheme.primaryContainer

            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )

            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                label = { Text("FILES", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )

            // Center Floating Action Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onCenterScanClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                label = { Text("TOOLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )

            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onTabSelected(4) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("USER", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}

/**
 * Real PIN lock gate. Shown whenever App Lock is enabled and the app was
 * just brought back to the foreground. The PIN is checked against the real
 * value saved in Profile > App Lock & Security (SharedPreferences) — there
 * is no bypass and no fixed/accepted code.
 */
@Composable
private fun AppLockScreen(
    onUnlock: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Enter your PIN to unlock ScanPro AI",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6) {
                        pin = it.filter { c -> c.isDigit() }
                        error = false
                    }
                },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            if (error) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Incorrect PIN, try again.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (onUnlock(pin)) {
                        onUnlocked()
                    } else {
                        error = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Unlock", fontWeight = FontWeight.Bold)
            }
        }
    }
}