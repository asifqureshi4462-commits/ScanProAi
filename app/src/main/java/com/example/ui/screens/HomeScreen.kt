package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TealLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ScanProViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit,
    onNavigateToTools: () -> Unit,
    onImportFileClicked: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val storageUsed by viewModel.storageUsedBytes.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val totalStorage = currentUser?.storageLimitBytes ?: 100_000_000_000L
    val usedBytes = storageUsed ?: 72_400_000_000L
    val usedGb = (usedBytes.toDouble() / (1024 * 1024 * 1024))
    val totalGb = (totalStorage.toDouble() / (1024 * 1024 * 1024)).toInt()
    val progress = (usedBytes.toFloat() / totalStorage.toFloat()).coerceIn(0.05f, 1.0f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Header Section (ScanPro AI Logo + User Profile)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanPrimary, NeonTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ScanPro ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = CyanPrimary
                            )
                        }
                        Text(
                            text = "PREMIUM SUITE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassCardBg)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0x3322D3EE), CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.displayName?.take(2)?.uppercase() ?: "JD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }
            }
        }

        // Search & Cloud Storage Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search documents...", color = TextMuted, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cloud Storage Indicator
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloud Storage",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f GB / %d GB", usedGb, totalGb),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(CyanPrimary, TealLight)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Quick Tools Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PDF Tools Card
                QuickToolGridCard(
                    title = "PDF Tools",
                    subtitle = "Convert & Edit",
                    icon = Icons.Default.PictureAsPdf,
                    iconBg = Color(0x3322D3EE),
                    iconTint = CyanPrimary,
                    isHighlight = true,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTools
                )

                // AI Chat Card
                QuickToolGridCard(
                    title = "AI Chat",
                    subtitle = "Analyze Docs",
                    icon = Icons.Default.AutoAwesome,
                    iconBg = Color(0x330D9488),
                    iconTint = TealLight,
                    isHighlight = false,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAiChat
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // OCR Text Card
                QuickToolGridCard(
                    title = "OCR Text",
                    subtitle = "Extract Data",
                    icon = Icons.Default.Translate,
                    iconBg = Color(0x33A855F7),
                    iconTint = NeonPurple,
                    isHighlight = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val firstDoc = documents.firstOrNull()
                        if (firstDoc != null) {
                            viewModel.runAiOcrOnDocument(firstDoc)
                            onNavigateToDocPreview(firstDoc)
                        } else {
                            onNavigateToScan()
                        }
                    }
                )

                // Converter Card
                QuickToolGridCard(
                    title = "Converter",
                    subtitle = "DOCX, PPTX, JPG",
                    icon = Icons.Default.UploadFile,
                    iconBg = Color(0x33F59E0B),
                    iconTint = NeonAmber,
                    isHighlight = false,
                    modifier = Modifier.weight(1f),
                    onClick = onImportFileClicked
                )
            }
        }

        // Recent Scans Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SCANS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextMuted
                )
                Text(
                    text = "View All",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToTools() }
                )
            }
        }

        // Recent Scans List
        if (documents.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No documents scanned yet", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(documents.take(5)) { doc ->
                DocumentListItem(
                    document = doc,
                    onClick = {
                        viewModel.setActiveDocument(doc)
                        onNavigateToDocPreview(doc)
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                    onDelete = { viewModel.deleteDocument(doc) },
                    onShare = {
                        val file = File(doc.filePath)
                        if (file.exists()) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF"))
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
fun QuickToolGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isHighlight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) Color(0x1B0891B2) else DarkSurface
        ),
        modifier = modifier
            .border(
                1.dp,
                if (isHighlight) Color(0x3322D3EE) else GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DocumentListItem(
    document: ScannedDocument,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isPdf = document.fileType.equals("PDF", ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPdf) Color(0x33EF4444) else Color(0x333B82F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPdf) "PDF" else "IMG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPdf) Color(0xFFF87171) else Color(0xFF60A5FA)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formattedSize = if (document.fileSizeBytes > 1_048_576) {
                        String.format(Locale.US, "%.1f MB", document.fileSizeBytes.toDouble() / 1048576)
                    } else {
                        "${(document.fileSizeBytes / 1024)} KB"
                    }
                    Text(
                        text = "$formattedSize • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(document.createdAt))}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (document.isFavorite) NeonAmber else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open & Preview", color = TextPrimary, fontSize = 13.sp) },
                        onClick = { showMenu = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Document", color = TextPrimary, fontSize = 13.sp) },
                        onClick = { showMenu = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Document", color = MaterialTheme.colorScheme.error, fontSize = 13.sp) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

