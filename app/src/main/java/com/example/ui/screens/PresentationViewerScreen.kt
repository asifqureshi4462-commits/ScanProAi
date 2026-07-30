package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SlideItem(
    var title: String,
    var subtitle: String,
    var bulletPoints: MutableList<String>,
    var speakerNotes: String,
    val bgGradientColor: Color = Color(0xFF0F172A)
)

@Composable
fun PresentationViewerScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val activeDocument by viewModel.activeDocument.collectAsState()
    val doc = activeDocument ?: return

    val slides = remember {
        mutableStateListOf(
            SlideItem(
                title = "ScanPro AI Office Suite",
                subtitle = "Next-Generation Document Intelligence",
                bulletPoints = mutableListOf("AI Powered Document Scanning", "PDF, Word, Excel & PPT Office Suite", "Real-time Gemini Cloud Synthesis"),
                speakerNotes = "Welcome the team and introduce key highlights."
            ),
            SlideItem(
                title = "Key Architectural Upgrades",
                subtitle = "Material 3 Design System & Room DB",
                bulletPoints = mutableListOf("Modular Jetpack Compose workflow screens", "Full offline file persistence & local caching", "Cross-format document conversion"),
                speakerNotes = "Focus on zero crash architecture."
            ),
            SlideItem(
                title = "Quarterly Goals & Roadmap",
                subtitle = "2026 Executive Summary",
                bulletPoints = mutableListOf("100% test coverage for critical paths", "Sub-100ms document rendering", "Enterprise security encryption"),
                speakerNotes = "Emphasize speed and reliability."
            )
        )
    }

    var currentSlideIndex by remember { mutableStateOf(0) }
    var isPresentationMode by remember { mutableStateOf(false) }
    var showSpeakerNotes by remember { mutableStateOf(false) }

    // Laser pointer position in presentation mode
    var laserPos by remember { mutableStateOf<Offset?>(null) }
    var isLaserActive by remember { mutableStateOf(false) }

    // Timer state
    var presentationSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isPresentationMode) {
        if (isPresentationMode) {
            presentationSeconds = 0
            while (isPresentationMode) {
                delay(1000)
                presentationSeconds++
            }
        }
    }

    val currentSlide = slides.getOrNull(currentSlideIndex) ?: slides[0]

    // Edit Slide Dialog
    var showEditSlideDialog by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf("") }
    var editSubtitleText by remember { mutableStateOf("") }

    if (showEditSlideDialog) {
        AlertDialog(
            onDismissRequest = { showEditSlideDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Slide Content", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        label = { Text("Slide Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSubtitleText,
                        onValueChange = { editSubtitleText = it },
                        label = { Text("Slide Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentSlide.title = editTitleText
                        currentSlide.subtitle = editSubtitleText
                        showEditSlideDialog = false
                        Toast.makeText(context, "Slide updated!", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditSlideDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Full Screen Presentation Mode
    if (isPresentationMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentSlide.bgGradientColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isLaserActive = true
                            laserPos = offset
                        },
                        onDrag = { change, _ ->
                            laserPos = change.position
                        },
                        onDragEnd = {
                            isLaserActive = false
                            laserPos = null
                        }
                    )
                }
        ) {
            // Main Slide Display
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentSlide.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentSlide.subtitle,
                    fontSize = 18.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    currentSlide.bulletPoints.forEach { point ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(point, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }

            // Laser Pointer Canvas Overlay
            if (isLaserActive && laserPos != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color.Red, radius = 16f, center = laserPos!!)
                    drawCircle(color = Color.White, radius = 6f, center = laserPos!!)
                }
            }

            // Presentation Top HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val mins = presentationSeconds / 60
                    val secs = presentationSeconds % 60
                    Text(String.format("%02d:%02d", mins, secs), color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                IconButton(onClick = { isPresentationMode = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Slideshow", tint = Color.White)
                }
            }

            // Slide Navigation Controls (Bottom)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Slide", tint = Color.White)
                }

                Text("Slide ${currentSlideIndex + 1} / ${slides.size}", color = Color.White, fontWeight = FontWeight.Bold)

                IconButton(
                    onClick = { if (currentSlideIndex < slides.size - 1) currentSlideIndex++ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Slide", tint = Color.White)
                }
            }
        }
        return
    }

    // Normal Editor / Viewer Mode
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar
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
                        .background(Color(0xFFEA580C).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Slideshow, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("PowerPoint Presentation Suite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row {
                IconButton(onClick = { isPresentationMode = true }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Slideshow", tint = Color(0xFF16A34A))
                }

                IconButton(
                    onClick = {
                        editTitleText = currentSlide.title
                        editSubtitleText = currentSlide.subtitle
                        showEditSlideDialog = true
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Slide")
                }

                IconButton(
                    onClick = {
                        Toast.makeText(context, "Presentation saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Slide Canvas Center Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = currentSlide.bgGradientColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(currentSlide.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentSlide.subtitle, fontSize = 14.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentSlide.bulletPoints.forEach { pt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pt, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Slide Thumbnail Strip & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Slides (${slides.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row {
                    TextButton(
                        onClick = {
                            slides.add(
                                SlideItem(
                                    title = "New Slide #${slides.size + 1}",
                                    subtitle = "Tap to edit topic details",
                                    bulletPoints = mutableListOf("Key point 1", "Key point 2"),
                                    speakerNotes = "Add presentation notes"
                                )
                            )
                            currentSlideIndex = slides.size - 1
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Slide", fontSize = 12.sp)
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(slides) { idx, slide ->
                    val isSelected = idx == currentSlideIndex
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = slide.bgGradientColor),
                        modifier = Modifier
                            .size(width = 110.dp, height = 75.dp)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { currentSlideIndex = idx }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(slide.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
