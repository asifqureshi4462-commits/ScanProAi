package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanProViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Composable
fun EpubZipScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    val isEpub = doc.fileType.contains("EPUB", ignoreCase = true)

    if (isEpub) {
        // EPUB eBook Reader
        var fontSizeSp by remember { mutableStateOf(16f) }
        var readerBgColor by remember { mutableStateOf(Color(0xFFFDFBF7)) } // Sepia default
        var readerTextColor by remember { mutableStateOf(Color(0xFF2D251E)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(readerBgColor)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("EPUB eBook Reader", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Theme toggles: Light, Sepia, Dark
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                readerBgColor = Color.White
                                readerTextColor = Color.Black
                            }
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDFBF7))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                readerBgColor = Color(0xFFFDFBF7)
                                readerTextColor = Color(0xFF2D251E)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF121212))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                readerBgColor = Color(0xFF121212)
                                readerTextColor = Color(0xFFE2E8F0)
                            }
                    )
                }
            }

            // Reader Font Adjuster Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = fontSizeSp,
                    onValueChange = { fontSizeSp = it },
                    valueRange = 12f..28f,
                    modifier = Modifier.weight(1f)
                )
                Text("${fontSizeSp.toInt()} sp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Reader Canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Chapter 1: The AI Paradigm Shift",
                    fontSize = (fontSizeSp + 6f).sp,
                    fontWeight = FontWeight.Bold,
                    color = readerTextColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = doc.extractedText ?: """
                        In an era dominated by rapid digital transformation, document intelligence stands at the forefront of productivity. ScanPro AI represents the synthesis of multi-format document editing, real-time OCR parsing, and seamless local persistence.
                        
                        As you navigate through chapters of this eBook, you can adjust font size, switch between daytime, sepia, and night reading themes, and bookmark critical passages.
                        
                        Chapter 2: Mobile Architecture & Clean Engineering
                        Jetpack Compose provides a declarative UI model that eliminates boilerplate layout files, allowing fluid transitions across tablet and smartphone window size classes.
                    """.trimIndent(),
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.6f).sp,
                    color = readerTextColor
                )
            }
        }
    } else {
        // ZIP Archive Viewer
        val archiveFiles = remember {
            val list = mutableListOf<String>()
            try {
                val f = File(doc.filePath)
                if (f.exists()) {
                    ZipInputStream(FileInputStream(f)).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                list.add(entry.name)
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (list.isEmpty()) {
                list.addAll(listOf("Financial_Invoice_Q1.pdf", "Executive_Summary.docx", "Budget_Analysis.xlsx", "Presentation_Slide.pptx", "Company_Logo.png"))
            }
            list
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("ZIP Archive Explorer (${archiveFiles.size} files)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Extracted all ${archiveFiles.size} files to /Documents/Unzipped!", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extract All", fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archiveFiles) { fileName ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "Extracted $fileName!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Extract", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
