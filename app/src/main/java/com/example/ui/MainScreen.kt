package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ScannedDocument
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DocumentPreviewScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerCameraScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
    MAIN_TABS,
    SCANNER_CAMERA,
    AI_CHAT,
    DOC_PREVIEW
}

@Composable
fun MainScreen(viewModel: ScanProViewModel = viewModel()) {
    val context = LocalContext.current

    var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Files, 2: Scan, 3: Tools, 4: Profile

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                Box(modifier = Modifier.padding(padding)) {
                    AiChatScreen(viewModel = viewModel)
                }
            }
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
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg),
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
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { currentDestination = AppDestination.SCANNER_CAMERA },
                            onNavigateToAiChat = { currentDestination = AppDestination.AI_CHAT },
                            onNavigateToDocPreview = { doc ->
                                viewModel.setActiveDocument(doc)
                                currentDestination = AppDestination.DOC_PREVIEW
                            },
                            onNavigateToTools = { selectedTab = 3 },
                            onImportFileClicked = { fileImportLauncher.launch("application/pdf") }
                        )

                        1 -> FilesScreen(
                            viewModel = viewModel,
                            onNavigateToDocPreview = { doc ->
                                viewModel.setActiveDocument(doc)
                                currentDestination = AppDestination.DOC_PREVIEW
                            }
                        )

                        3 -> ToolsScreen(
                            viewModel = viewModel,
                            onNavigateToDocPreview = { doc ->
                                viewModel.setActiveDocument(doc)
                                currentDestination = AppDestination.DOC_PREVIEW
                            }
                        )

                        4 -> ProfileScreen(
                            viewModel = viewModel,
                            onNavigateToAuth = { currentDestination = AppDestination.AUTH }
                        )

                        else -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { currentDestination = AppDestination.SCANNER_CAMERA },
                            onNavigateToAiChat = { currentDestination = AppDestination.AI_CHAT },
                            onNavigateToDocPreview = { doc ->
                                viewModel.setActiveDocument(doc)
                                currentDestination = AppDestination.DOC_PREVIEW
                            },
                            onNavigateToTools = { selectedTab = 3 },
                            onImportFileClicked = { fileImportLauncher.launch("application/pdf") }
                        )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        NavigationBar(
            containerColor = DarkSurface,
            contentColor = CyanPrimary,
            tonalElevation = 8.dp,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(72.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = Color(0x2222D3EE),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )

            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                label = { Text("FILES", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = Color(0x2222D3EE),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )

            // Center Spacer for Floating FAB
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
                        .background(DarkBg)
                        .border(3.dp, GlassBorder, CircleShape)
                        .clickable { onCenterScanClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanPrimary, NeonTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan",
                            tint = Color.White,
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
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = Color(0x2222D3EE),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )

            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onTabSelected(4) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("USER", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = Color(0x2222D3EE),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
