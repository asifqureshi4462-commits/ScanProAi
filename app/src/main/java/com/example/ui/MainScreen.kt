package com.example.ui

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
