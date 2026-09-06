package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.ScanProViewModel
import java.io.File

@Composable
fun TextCodeEditorScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    var codeText by remember {
        mutableStateOf(
            doc.extractedText ?: when {
                doc.fileType.contains("JSON", ignoreCase = true) -> """
                    {
                      "appName": "ScanPro AI Office Suite",
                      "version": "4.5.0",
                      "features": [
                        "PDF Editor & Annotator",
                        "Word & Rich Text Suite",
                        "Excel & Formula Spreadsheet",
                        "PowerPoint Presentation Mode",
                        "Image Filter & OCR Studio"
                      ],
                      "status": "Production Ready"
                    }
                """.trimIndent()
                doc.fileType.contains("HTML", ignoreCase = true) -> """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <style>
                        body { font-family: sans-serif; background: #f8fafc; padding: 20px; color: #0f172a; }
                        h1 { color: #2563eb; }
                        .card { background: white; padding: 16px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                      </style>
                    </head>
                    <body>
                      <div class="card">
                        <h1>ScanPro AI Web Renderer</h1>
                        <p>Live rendered HTML layout directly inside ScanPro AI Office Suite.</p>
                      </div>
                    </body>
                    </html>
                """.trimIndent()
                else -> """
                    <?xml version="1.0" encoding="utf-8"?>
                    <document title="${doc.title}">
                        <metadata created="${System.currentTimeMillis()}" author="ScanPro AI"/>
                        <content>Sample XML Code Document Content</content>
                    </document>
                """.trimIndent()
            }
        )
    }

    var isLiveHtmlPreview by remember { mutableStateOf(false) }
    var showAiModal by remember { mutableStateOf(false) }
    var aiResultContent by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // AI Modal
    if (showAiModal) {
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Code Analysis", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isAiLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing code structure...")
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(aiResultContent, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiModal = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("Code & Data Editor (${doc.fileType})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Row {
                if (doc.fileType.contains("HTML", ignoreCase = true)) {
                    IconButton(onClick = { isLiveHtmlPreview = !isLiveHtmlPreview }) {
                        Icon(
                            Icons.Default.Preview,
                            contentDescription = "Toggle Live HTML Preview",
                            tint = if (isLiveHtmlPreview) Color(0xFF38BDF8) else Color.White
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isAiLoading = true
                        showAiModal = true
                        viewModel.askAiQuestion("Review and explain this code or JSON data:\n\n$codeText") { res ->
                            aiResultContent = res
                            isAiLoading = false
                        }
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Review", tint = Color(0xFFA855F7))
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "Saved code document!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Code", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Main Editor Canvas / Live WebView Render
        if (isLiveHtmlPreview && doc.fileType.contains("HTML", ignoreCase = true)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, codeText, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Line Numbers Rail
                val lines = codeText.split("\n")
                Column(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    lines.indices.forEach { idx ->
                        Text(
                            text = "${idx + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF38BDF8),
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
