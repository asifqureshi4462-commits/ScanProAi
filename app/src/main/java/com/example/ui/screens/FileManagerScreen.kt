package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    viewModel: ScanProViewModel,
    onOpenDocument: (ScannedDocument) -> Unit,
    onBackClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.viewModeGrid.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var activeStorageTab by remember { mutableStateOf(0) } // 0: Internal, 1: SD Card, 2: Recent, 3: Downloads, 4: Favorites, 5: Cloud
    var selectedFileTypeFilter by remember { mutableStateOf("All") } // All, PDF, Word, Excel, PPT, Image, Code, ZIP, EPUB
    var sortOrder by remember { mutableStateOf("Date") } // Date, Name, Size, Type
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedForMulti by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var documentToRename by remember { mutableStateOf<ScannedDocument?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    val fileTypesList = listOf("All", "PDF", "Word", "Excel", "PPT", "Image", "Code", "ZIP", "EPUB")

    // Filter documents based on storage tab, file type filter, search query & sort
    val filteredDocs = remember(documents, activeStorageTab, selectedFileTypeFilter, searchQuery, sortOrder) {
        var list = documents

        // Tab filter
        when (activeStorageTab) {
            2 -> list = list.sortedByDescending { it.updatedAt } // Recent
            4 -> list = list.filter { it.isFavorite } // Favorites
            5 -> list = list.filter { it.category == "Cloud Folder" } // Cloud
        }

        // Format filter
        if (selectedFileTypeFilter != "All") {
            list = list.filter { doc ->
                val ext = doc.fileType.uppercase()
                when (selectedFileTypeFilter) {
                    "PDF" -> ext.contains("PDF")
                    "Word" -> ext.contains("DOC") || ext.contains("TXT") || ext.contains("RTF")
                    "Excel" -> ext.contains("XLS") || ext.contains("CSV")
                    "PPT" -> ext.contains("PPT")
                    "Image" -> ext.contains("JPG") || ext.contains("PNG") || ext.contains("WEBP") || ext.contains("JPEG")
                    "Code" -> ext.contains("JSON") || ext.contains("HTML") || ext.contains("XML")
                    "ZIP" -> ext.contains("ZIP")
                    "EPUB" -> ext.contains("EPUB")
                    else -> true
                }
            }
        }

        // Search
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

        // Sorting
        when (sortOrder) {
            "Name" -> list.sortedBy { it.title.lowercase() }
            "Size" -> list.sortedByDescending { it.fileSizeBytes }
            "Type" -> list.sortedBy { it.fileType }
            else -> list.sortedByDescending { it.createdAt }
        }
    }

    // Rename Dialog
    if (documentToRename != null) {
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Rename File", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        documentToRename?.let { doc ->
                            if (renameInputText.isNotBlank()) {
                                viewModel.renameDocument(doc, renameInputText.trim())
                                Toast.makeText(context, "Renamed to $renameInputText", Toast.LENGTH_SHORT).show()
                            }
                        }
                        documentToRename = null
                    }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Create Directory / Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    placeholder = { Text("Folder Name (e.g., Office Reports)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.selectedCategory.value = folderNameInput
                            Toast.makeText(context, "Folder created: $folderNameInput", Toast.LENGTH_SHORT).show()
                        }
                        showCreateFolderDialog = false
                        folderNameInput = ""
                    }
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Storage Selector Bar
        ScrollableTabRow(
            selectedTabIndex = activeStorageTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            val tabs = listOf(
                "Internal" to Icons.Default.Storage,
                "SD Card" to Icons.Default.SdCard,
                "Recent" to Icons.Default.Download,
                "Downloads" to Icons.Default.Folder,
                "Favorites" to Icons.Default.Star,
                "Cloud" to Icons.Default.Cloud
            )
            tabs.forEachIndexed { index, pair ->
                Tab(
                    selected = activeStorageTab == index,
                    onClick = { activeStorageTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(pair.second, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(pair.first, fontSize = 13.sp, fontWeight = if (activeStorageTab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        // Search & Filter Row
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClicked != null) {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search files & documents...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        listOf("Date", "Name", "Size", "Type").forEach { option ->
                            DropdownMenuItem(
                                text = { Text("Sort by $option") },
                                onClick = {
                                    sortOrder = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.viewModeGrid.value = !isGridView },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File Type Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fileTypesList) { type ->
                    val isSelected = selectedFileTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFileTypeFilter = type },
                        label = { Text(type, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Batch Action Bar if items selected
        if (selectedForMulti.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${selectedForMulti.size} selected", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(
                        onClick = {
                            val selectedDocs = documents.filter { it.id in selectedForMulti }
                            viewModel.deleteMultipleDocuments(selectedDocs)
                            selectedForMulti = emptySet()
                            Toast.makeText(context, "Deleted selected files", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }

                    IconButton(
                        onClick = {
                            // ZIP compress selected files
                            val selectedDocs = documents.filter { it.id in selectedForMulti }
                            if (selectedDocs.isNotEmpty()) {
                                createZipFromDocs(context, selectedDocs, viewModel)
                                selectedForMulti = emptySet()
                            }
                        }
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = "Compress to ZIP", tint = MaterialTheme.colorScheme.primary)
                    }

                    TextButton(onClick = { selectedForMulti = emptySet() }) {
                        Text("Deselect", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Document List / Grid
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No files found in this directory", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDocs) { doc ->
                    FileManagerGridCard(
                        doc = doc,
                        isSelected = doc.id in selectedForMulti,
                        onClick = {
                            if (selectedForMulti.isNotEmpty()) {
                                selectedForMulti = if (doc.id in selectedForMulti) selectedForMulti - doc.id else selectedForMulti + doc.id
                            } else {
                                onOpenDocument(doc)
                            }
                        },
                        onLongClick = {
                            selectedForMulti = selectedForMulti + doc.id
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(doc) },
                        onRenameClick = {
                            documentToRename = doc
                            renameInputText = doc.title
                        },
                        onDeleteClick = { viewModel.deleteDocument(doc) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDocs) { doc ->
                    FileManagerListItem(
                        doc = doc,
                        isSelected = doc.id in selectedForMulti,
                        onClick = {
                            if (selectedForMulti.isNotEmpty()) {
                                selectedForMulti = if (doc.id in selectedForMulti) selectedForMulti - doc.id else selectedForMulti + doc.id
                            } else {
                                onOpenDocument(doc)
                            }
                        },
                        onLongClick = {
                            selectedForMulti = selectedForMulti + doc.id
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(doc) },
                        onRenameClick = {
                            documentToRename = doc
                            renameInputText = doc.title
                        },
                        onDeleteClick = { viewModel.deleteDocument(doc) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerListItem(
    doc: ScannedDocument,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val iconPair = getIconAndColorForFileType(doc.fileType)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconPair.second.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconPair.first, contentDescription = null, tint = iconPair.second, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.fileType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconPair.second
                    )
                    Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatFileSize(doc.fileSizeBytes),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(doc.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = if (doc.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.List, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerGridCard(
    doc: ScannedDocument,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val iconPair = getIconAndColorForFileType(doc.fileType)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconPair.second.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconPair.first, contentDescription = null, tint = iconPair.second, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (doc.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = doc.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = doc.fileType,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconPair.second
                )
                Text(
                    text = formatFileSize(doc.fileSizeBytes),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getIconAndColorForFileType(fileType: String): Pair<ImageVector, Color> {
    val ext = fileType.uppercase()
    return when {
        ext.contains("PDF") -> Pair(Icons.Default.PictureAsPdf, Color(0xFFEF4444))
        ext.contains("DOC") || ext.contains("TXT") || ext.contains("RTF") -> Pair(Icons.Default.Description, Color(0xFF2563EB))
        ext.contains("XLS") || ext.contains("CSV") -> Pair(Icons.Default.TableChart, Color(0xFF16A34A))
        ext.contains("PPT") -> Pair(Icons.Default.Slideshow, Color(0xFFEA580C))
        ext.contains("JPG") || ext.contains("PNG") || ext.contains("WEBP") || ext.contains("JPEG") -> Pair(Icons.Default.Image, Color(0xFF0EA5E9))
        ext.contains("JSON") || ext.contains("HTML") || ext.contains("XML") -> Pair(Icons.Default.Code, Color(0xFF8B5CF6))
        ext.contains("ZIP") -> Pair(Icons.Default.FolderZip, Color(0xFFD97706))
        else -> Pair(Icons.Default.Folder, Color(0xFF6B7280))
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun createZipFromDocs(context: Context, docs: List<ScannedDocument>, viewModel: ScanProViewModel) {
    try {
        val zipFile = File(context.filesDir, "Archive_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { out ->
            for (doc in docs) {
                val f = File(doc.filePath)
                if (f.exists()) {
                    val entry = ZipEntry(f.name)
                    out.putNextEntry(entry)
                    FileInputStream(f).use { input -> input.copyTo(out) }
                    out.closeEntry()
                }
            }
        }

        val zipDoc = ScannedDocument(
            title = zipFile.name,
            filePath = zipFile.absolutePath,
            fileType = "ZIP",
            fileSizeBytes = zipFile.length(),
            pageCount = docs.size,
            category = "Archives",
            createdAt = System.currentTimeMillis()
        )

        viewModel.addNewDocument(zipDoc)
        Toast.makeText(context, "Compressed ${docs.size} files into ZIP archive!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "ZIP Compression failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
