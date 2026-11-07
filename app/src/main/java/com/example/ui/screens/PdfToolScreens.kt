package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentRepository
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.CompressLevel
import com.example.util.PdfOpResult
import com.example.util.PdfProbe
import com.example.util.PdfToolsEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// =====================================================================
// STATE HOLDERS
// =====================================================================

/** A PDF that is ready to be processed (always readable, never password protected). */
class PdfSource(
    val title: String,
    val file: File,
    val pageCount: Int,
    val wasEncrypted: Boolean = false
) {
    val baseTitle: String
        get() = title.removeSuffix(".pdf").removeSuffix(".PDF")
}

class PendingPdf(val file: File, val title: String)

sealed interface ToolUiState {
    object Idle : ToolUiState
    data class Working(val message: String, val progress: Float) : ToolUiState
    data class Done(val docs: List<ScannedDocument>, val note: String?, val locked: Boolean) : ToolUiState
    data class Failed(val message: String) : ToolUiState
}

/** Runs one PDF operation, shows real progress and stores the result in the vault. */
class PdfToolRunner(
    private val repo: DocumentRepository,
    private val scope: CoroutineScope
) {
    var state: ToolUiState by mutableStateOf<ToolUiState>(ToolUiState.Idle)
        private set

    fun reset() {
        state = ToolUiState.Idle
    }

    fun fail(message: String) {
        state = ToolUiState.Failed(message)
    }

    fun run(
        message: String,
        category: String = "Documents",
        block: suspend (onProgress: (Float) -> Unit) -> PdfOpResult
    ) {
        if (state is ToolUiState.Working) return
        state = ToolUiState.Working(message, 0f)
        scope.launch {
            val result: PdfOpResult = try {
                block { p ->
                    if (state is ToolUiState.Working) state = ToolUiState.Working(message, p)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                PdfOpResult.Failure(PdfToolsEngine.friendlyError(t))
            }
            state = when (result) {
                is PdfOpResult.Failure -> ToolUiState.Failed(result.message)
                is PdfOpResult.Success -> {
                    val docs = ArrayList<ScannedDocument>()
                    result.files.forEachIndexed { i, f ->
                        val doc = ScannedDocument(
                            title = f.name,
                            filePath = f.absolutePath,
                            fileType = result.fileType,
                            fileSizeBytes = f.length(),
                            pageCount = result.pageCounts.getOrNull(i) ?: 1,
                            category = category,
                            isLocked = result.locked
                        )
                        val id = try {
                            repo.insertDocument(doc)
                        } catch (t: Throwable) {
                            0L
                        }
                        docs.add(doc.copy(id = id))
                    }
                    ToolUiState.Done(docs, result.note, result.locked)
                }
            }
        }
    }
}

@Composable
fun rememberPdfToolRunner(viewModel: ScanProViewModel): PdfToolRunner {
    val scope = rememberCoroutineScope()
    return remember { PdfToolRunner(viewModel.documentRepository, scope) }
}

@Composable
private fun rememberVaultPdfs(viewModel: ScanProViewModel): List<ScannedDocument> {
    val all by viewModel.documentRepository.allDocuments.collectAsState(initial = emptyList())
    return remember(all) {
        all.filter { it.fileType.equals("PDF", ignoreCase = true) && File(it.filePath).exists() }
    }
}

private suspend fun copyUriToWork(context: Context, uri: Uri): Pair<File, String>? =
    withContext(Dispatchers.IO) {
        try {
            var name = "document.pdf"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { name = it }
                }
            }
            val file = File(PdfToolsEngine.workDir(context), "src_${System.nanoTime()}.pdf")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { ins -> file.outputStream().use { ins.copyTo(it) } }
            Pair(file, name)
        } catch (e: Throwable) {
            null
        }
    }

// =====================================================================
// SHARED UI
// =====================================================================

@Composable
private fun ToolTopBar(title: String, description: String, icon: ImageVector, color: Color, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, label ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(label, fontSize = 13.sp) })
        }
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    error: String?,
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Password needed") },
        text = {
            Column {
                Text("\"$title\" is protected. Enter its password to continue.", fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("PDF password") },
                    singleLine = true,
                    isError = error != null,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.isNotEmpty() && !busy) { Text("Unlock") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}

@Composable
private fun VaultPickerDialog(
    docs: List<ScannedDocument>,
    multi: Boolean,
    onPick: (ScannedDocument) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (multi) "Add PDF from Vault" else "Choose a PDF") },
        text = {
            if (docs.isEmpty()) {
                Text("No PDF files in your vault yet. Use \"Browse phone\" instead.", fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(docs, key = { it.id }) { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { onPick(doc) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${doc.pageCount} pages • ${PdfToolsEngine.formatSize(doc.fileSizeBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * Prepares picked files: checks they are valid PDFs and asks for the password
 * of protected ones. Used by every single-file tool.
 */
@Composable
private fun PdfSourceCard(
    source: PdfSource?,
    vaultDocs: List<ScannedDocument>,
    onSourceChanged: (PdfSource?) -> Unit,
    notify: (String) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showVault by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingPdf?>(null) }
    var pwError by remember { mutableStateOf<String?>(null) }
    var pwBusy by remember { mutableStateOf(false) }

    suspend fun accept(file: File, title: String) {
        when (val p = PdfToolsEngine.probe(file)) {
            is PdfProbe.Ok -> onSourceChanged(PdfSource(title, file, p.pageCount))
            is PdfProbe.Encrypted -> {
                pwError = null
                pending = PendingPdf(file, title)
            }
            is PdfProbe.Invalid -> notify("This file is not a valid PDF or it is empty.")
        }
    }

    val deviceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                val copied = copyUriToWork(context, uri)
                if (copied == null) notify("Could not read that file.") else accept(copied.first, copied.second)
                busy = false
            }
        }
    }

    SectionCard("1. Choose your PDF") {
        if (source != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(source.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${source.pageCount} pages • ${PdfToolsEngine.formatSize(source.file.length())}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showVault = true }, enabled = enabled && !busy, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("Vault", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { deviceLauncher.launch(arrayOf("application/pdf")) },
                    enabled = enabled && !busy,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Phone", fontSize = 13.sp) }
            }
        } else {
            Button(
                onClick = { showVault = true },
                enabled = enabled && !busy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("From ScanPro Vault")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { deviceLauncher.launch(arrayOf("application/pdf")) },
                enabled = enabled && !busy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Browse phone storage")
            }
        }
        if (busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    if (showVault) {
        VaultPickerDialog(
            docs = vaultDocs,
            multi = false,
            onPick = { doc ->
                showVault = false
                scope.launch {
                    busy = true
                    accept(File(doc.filePath), doc.title)
                    busy = false
                }
            },
            onDismiss = { showVault = false }
        )
    }

    pending?.let { p ->
        PasswordDialog(
            title = p.title,
            error = pwError,
            busy = pwBusy,
            onConfirm = { pw ->
                scope.launch {
                    pwBusy = true
                    val out = File(PdfToolsEngine.workDir(context), "unlocked_${System.nanoTime()}.pdf")
                    when (val r = PdfToolsEngine.unlockToFile(context, p.file, pw, out)) {
                        is PdfOpResult.Success -> {
                            val probe = PdfToolsEngine.probe(out)
                            if (probe is PdfProbe.Ok) {
                                onSourceChanged(PdfSource(p.title, out, probe.pageCount, wasEncrypted = true))
                                pending = null
                            } else {
                                pwError = "Could not open this PDF."
                            }
                        }
                        is PdfOpResult.Failure -> {
                            out.delete()
                            pwError = r.message
                        }
                    }
                    pwBusy = false
                }
            },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun ResultCard(
    state: ToolUiState.Done,
    onOpen: (ScannedDocument) -> Unit,
    onReset: () -> Unit,
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember(state) { state.docs.map { File(it.filePath) } }
    SectionCard("Done!") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.docs.size == 1) "Your file is ready and saved in the Vault." else "${state.docs.size} files are ready and saved in the Vault.",
                fontSize = 14.sp
            )
        }
        if (state.note != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.note, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        if (state.locked) {
            Spacer(Modifier.height(8.dp))
            Text(
                "This PDF needs its password to open. Keep the password safe, it cannot be recovered.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        state.docs.take(5).forEach { d ->
            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(d.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${d.pageCount} page(s) • ${PdfToolsEngine.formatSize(d.fileSizeBytes)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state.docs.size > 5) {
            Text("+ ${state.docs.size - 5} more files", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        if (state.docs.size == 1 && !state.locked) {
            Button(
                onClick = { onOpen(state.docs[0]) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open file")
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { PdfToolsEngine.share(context, files) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { PdfToolsEngine.saveToDownloads(context, files) }
                        when {
                            n < 0 -> notify("Use Share to save the file on this Android version.")
                            n == 0 -> notify("Could not save the file.")
                            else -> notify("Saved $n file(s) in Downloads/ScanPro")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save to phone", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Process another file") }
    }
}

/**
 * Common page layout for every PDF tool: top bar, scrolling content, status
 * banner, snackbar messages and a big thumb-friendly action button at the bottom.
 * When [ownScroll] is true the content provides its own LazyColumn.
 */
@Composable
private fun PdfToolScaffold(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    runner: PdfToolRunner,
    actionLabel: String,
    actionEnabled: Boolean,
    onBack: () -> Unit,
    onOpenDocument: (ScannedDocument) -> Unit,
    onAction: () -> Unit,
    ownScroll: Boolean = false,
    content: @Composable ColumnScope.(notify: (String) -> Unit) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = remember(snackbar, scope) {
        { msg: String -> scope.launch { snackbar.showSnackbar(msg) }; Unit }
    }
    val state = runner.state

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            ToolTopBar(title, description, icon, color, onBack)

            when (state) {
                is ToolUiState.Done -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        ResultCard(state, onOpenDocument, { runner.reset() }, notify)
                    }
                }
                else -> {
                    if (state is ToolUiState.Failed) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { runner.reset() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                    if (state is ToolUiState.Working) {
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                            Text(state.message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            if (state.progress <= 0f) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    if (ownScroll) {
                        Column(modifier = Modifier.weight(1f)) { content(notify) }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) { content(notify) }
                    }
                    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                        Button(
                            onClick = onAction,
                            enabled = actionEnabled && state !is ToolUiState.Working,
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .height(56.dp)
                        ) {
                            if (state is ToolUiState.Working) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, label: String = "Output file name") {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        suffix = { Text(".pdf") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}

@Composable
private fun PdfPageThumb(file: File, pageIndex: Int, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(initialValue = null, file, pageIndex) {
        value = PdfToolsEngine.thumbnail(file, pageIndex, 260)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

// =====================================================================
// 1. MERGE
// =====================================================================

private class MergeItem(val title: String, val file: File, val pages: Int)

@Composable
fun PdfMergeScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    val items = remember { mutableStateListOf<MergeItem>() }
    val queue = remember { mutableStateListOf<PendingPdf>() }
    var outName by remember { mutableStateOf("Merged_Document") }
    var showVault by remember { mutableStateOf(false) }
    var pwError by remember { mutableStateOf<String?>(null) }
    var pwBusy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lastNotify by remember { mutableStateOf<(String) -> Unit>({}) }

    suspend fun addFile(file: File, title: String) {
        when (val p = PdfToolsEngine.probe(file)) {
            is PdfProbe.Ok -> items.add(MergeItem(title, file, p.pageCount))
            is PdfProbe.Encrypted -> queue.add(PendingPdf(file, title))
            is PdfProbe.Invalid -> lastNotify("\"$title\" is not a valid PDF and was skipped.")
        }
    }

    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                busy = true
                uris.forEach { uri ->
                    val copied = copyUriToWork(context, uri)
                    if (copied != null) addFile(copied.first, copied.second)
                }
                busy = false
            }
        }
    }

    PdfToolScaffold(
        title = "Merge PDFs",
        description = "Combine several PDFs into one file",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFFEC4899),
        runner = runner,
        actionLabel = if (items.size >= 2) "Merge ${items.size} PDFs" else "Add at least 2 PDFs",
        actionEnabled = items.size >= 2 && outName.isNotBlank() && !busy,
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val files = items.map { it.file }
            runner.run("Merging ${files.size} PDFs...") { progress ->
                PdfToolsEngine.merge(context, files, outName, progress)
            }
        }
    ) { notify ->
        lastNotify = notify
        SectionCard("1. Add PDFs (${items.size} added)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showVault = true },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Vault", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { multiLauncher.launch(arrayOf("application/pdf")) },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Phone", fontSize = 13.sp)
                }
            }
            if (busy) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (items.isEmpty()) {
            EmptyHint("No PDFs added yet. Files are merged in the order shown here.")
        } else {
            SectionCard("2. Order (top = first)") {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${item.pages} pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val t = items[index]
                                    items[index] = items[index - 1]
                                    items[index - 1] = t
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(44.dp)
                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            onClick = {
                                if (index < items.size - 1) {
                                    val t = items[index]
                                    items[index] = items[index + 1]
                                    items[index + 1] = t
                                }
                            },
                            enabled = index < items.size - 1,
                            modifier = Modifier.size(44.dp)
                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                        IconButton(onClick = { items.removeAt(index) }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Total: ${items.sumOf { it.pages }} pages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NameField(outName, { outName = it })
        }
    }

    if (showVault) {
        VaultPickerDialog(
            docs = vaultDocs,
            multi = true,
            onPick = { doc ->
                showVault = false
                scope.launch {
                    busy = true
                    addFile(File(doc.filePath), doc.title)
                    busy = false
                }
            },
            onDismiss = { showVault = false }
        )
    }

    val current = queue.firstOrNull()
    if (current != null) {
        PasswordDialog(
            title = current.title,
            error = pwError,
            busy = pwBusy,
            onConfirm = { pw ->
                scope.launch {
                    pwBusy = true
                    val out = File(PdfToolsEngine.workDir(context), "unlocked_${System.nanoTime()}.pdf")
                    when (val r = PdfToolsEngine.unlockToFile(context, current.file, pw, out)) {
                        is PdfOpResult.Success -> {
                            val probe = PdfToolsEngine.probe(out)
                            if (probe is PdfProbe.Ok) items.add(MergeItem(current.title, out, probe.pageCount))
                            queue.remove(current)
                            pwError = null
                        }
                        is PdfOpResult.Failure -> {
                            out.delete()
                            pwError = r.message
                        }
                    }
                    pwBusy = false
                }
            },
            onDismiss = {
                queue.remove(current)
                pwError = null
            }
        )
    }
}

// =====================================================================
// 2. SPLIT
// =====================================================================

private class SplitPlan(val groups: List<List<Int>>, val problem: String?)

private fun computeSplitPlan(mode: Int, rangeText: String, combine: Boolean, everyN: String, total: Int): SplitPlan {
    var groups: List<List<Int>> = emptyList()
    var problem: String? = null
    when (mode) {
        0 -> {
            if (rangeText.isNotBlank()) {
                val parsed = PdfToolsEngine.parseRanges(rangeText, total)
                problem = parsed.error
                groups = if (combine && parsed.error == null) listOf(parsed.groups.flatten()) else parsed.groups
            }
        }
        1 -> {
            val n = everyN.toIntOrNull()
            if (n == null || n < 1) problem = "Enter a number of 1 or more."
            else if (n >= total) problem = "This PDF has only $total pages, choose a smaller number."
            else groups = (1..total).toList().chunked(n)
        }
        else -> groups = (1..total).map { listOf(it) }
    }
    if (problem == null && groups.size > 100) {
        problem = "That would create ${groups.size} files (maximum 100). Use bigger parts."
        groups = emptyList()
    }
    return SplitPlan(groups, problem)
}

@Composable
fun PdfSplitScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 ranges, 1 every N, 2 each page
    var rangeText by remember { mutableStateOf("") }
    var combine by remember { mutableStateOf(false) }
    var everyN by remember { mutableStateOf("2") }

    val src = source
    val total = src?.pageCount ?: 0
    val plan = if (src != null) computeSplitPlan(mode, rangeText, combine, everyN, total) else SplitPlan(emptyList(), null)
    val groups = plan.groups
    val problem = plan.problem

    PdfToolScaffold(
        title = "Split PDF",
        description = "Extract page ranges or cut into parts",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF0284C7),
        runner = runner,
        actionLabel = if (groups.isNotEmpty()) "Create ${groups.size} file(s)" else "Choose what to extract",
        actionEnabled = src != null && groups.isNotEmpty() && problem == null,
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            runner.run("Splitting PDF...") { progress ->
                PdfToolsEngine.buildFromPages(context, s.file, groups, s.baseTitle, progress)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it; rangeText = "" }, notify)
        if (src != null) {
            SectionCard("2. How to split") {
                ChipRow(listOf("Page ranges", "Every N pages", "Each page"), mode) { mode = it }
                Spacer(Modifier.height(12.dp))
                when (mode) {
                    0 -> {
                        OutlinedTextField(
                            value = rangeText,
                            onValueChange = { rangeText = it },
                            label = { Text("Pages, e.g. 1-3, 5, 8-10") },
                            singleLine = true,
                            isError = problem != null,
                            supportingText = {
                                Text(problem ?: "This PDF has $total pages. Each comma part becomes its own file.")
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Put all ranges in one PDF", fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Switch(checked = combine, onCheckedChange = { combine = it })
                        }
                    }
                    1 -> OutlinedTextField(
                        value = everyN,
                        onValueChange = { v -> everyN = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Pages per file") },
                        singleLine = true,
                        isError = problem != null,
                        supportingText = { Text(problem ?: "Creates ${groups.size} file(s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> Text(
                        problem ?: "Creates $total separate one-page PDFs.",
                        fontSize = 13.sp,
                        color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =====================================================================
// 3. COMPRESS
// =====================================================================

@Composable
fun PdfCompressScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var level by remember { mutableStateOf(CompressLevel.BALANCED) }
    var outName by remember(source) { mutableStateOf("${source?.baseTitle ?: "Document"}_compressed") }

    PdfToolScaffold(
        title = "Compress PDF",
        description = "Make the file smaller for sharing",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF14B8A6),
        runner = runner,
        actionLabel = "Compress PDF",
        actionEnabled = source != null && outName.isNotBlank(),
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            runner.run("Compressing images...") { progress ->
                PdfToolsEngine.compress(context, s.file, level, outName, progress)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        if (source != null) {
            SectionCard("2. Compression level") {
                CompressLevel.values().forEach { l ->
                    val selected = l == level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { level = l }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(l.label, fontWeight = FontWeight.Bold)
                            Text(l.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Works best on scanned or photo PDFs. If the PDF cannot get smaller you will be told.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NameField(outName, { outName = it })
        }
    }
}

// =====================================================================
// 4. PROTECT
// =====================================================================

@Composable
fun PdfProtectScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var outName by remember(source) { mutableStateOf("${source?.baseTitle ?: "Document"}_protected") }

    val mismatch = confirm.isNotEmpty() && confirm != password
    val tooShort = password.isNotEmpty() && password.length < 4
    val ready = source != null && password.length >= 4 && password == confirm && outName.isNotBlank()

    PdfToolScaffold(
        title = "Protect PDF",
        description = "Lock the PDF with a password",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFFF59E0B),
        runner = runner,
        actionLabel = "Encrypt & Save",
        actionEnabled = ready,
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            val pw = password
            runner.run("Encrypting PDF...") { _ ->
                PdfToolsEngine.protect(context, s.file, pw, outName)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        if (source != null) {
            SectionCard("2. Set a password") {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 4 characters)") },
                    singleLine = true,
                    isError = tooShort,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = { if (mismatch) Text("Passwords do not match") },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "AES-128 encryption. Remember this password, it cannot be recovered.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NameField(outName, { outName = it })
        }
    }
}

// =====================================================================
// 5. UNLOCK
// =====================================================================

@Composable
fun PdfUnlockScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var outName by remember(source) { mutableStateOf("${source?.baseTitle ?: "Document"}_unlocked") }

    PdfToolScaffold(
        title = "Unlock PDF",
        description = "Remove the password from a PDF",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF10B981),
        runner = runner,
        actionLabel = "Save unlocked copy",
        actionEnabled = source?.wasEncrypted == true && outName.isNotBlank(),
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            runner.run("Saving unlocked copy...") { _ ->
                PdfToolsEngine.saveCopy(context, s.file, outName)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        val s = source
        if (s == null) {
            EmptyHint("Pick a password protected PDF. You will be asked for its password, then you can save an unlocked copy.")
        } else if (!s.wasEncrypted) {
            SectionCard("Not protected") {
                Text("This PDF has no password, so there is nothing to unlock.", fontSize = 14.sp)
            }
        } else {
            SectionCard("Password accepted") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(Modifier.width(8.dp))
                    Text("The PDF is open. Save a copy without a password.", fontSize = 14.sp)
                }
            }
            NameField(outName, { outName = it })
        }
    }
}

// =====================================================================
// 6. WATERMARK
// =====================================================================

@Composable
fun PdfWatermarkScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var text by remember { mutableStateOf("CONFIDENTIAL") }
    var opacity by remember { mutableStateOf(0.25f) }
    var sizeIdx by remember { mutableStateOf(1) }
    var colorIdx by remember { mutableStateOf(0) }
    var diagonal by remember { mutableStateOf(true) }
    var outName by remember(source) { mutableStateOf("${source?.baseTitle ?: "Document"}_watermarked") }

    val colorNames = listOf("Gray", "Red", "Blue", "Black")
    val colors = listOf(Triple(128, 128, 128), Triple(220, 38, 38), Triple(37, 99, 235), Triple(0, 0, 0))
    val sizes = listOf(0.45f, 0.65f, 0.85f)
    val textOk = PdfToolsEngine.isWatermarkTextSupported(text.trim())

    PdfToolScaffold(
        title = "Watermark PDF",
        description = "Stamp text on every page",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF22D3EE),
        runner = runner,
        actionLabel = "Add watermark",
        actionEnabled = source != null && textOk && outName.isNotBlank(),
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            runner.run("Adding watermark...") { progress ->
                PdfToolsEngine.watermark(
                    context, s.file, text, opacity, sizes[sizeIdx], colors[colorIdx], diagonal, outName, progress
                )
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        if (source != null) {
            SectionCard("2. Watermark text") {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    label = { Text("Text") },
                    singleLine = true,
                    isError = !textOk,
                    supportingText = {
                        Text(if (textOk) "${text.length}/40" else "Use English letters, numbers and symbols only.")
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val c = colors[colorIdx]
                    Text(
                        text.ifBlank { "Preview" },
                        fontSize = (16 + sizeIdx * 6).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(c.first, c.second, c.third).copy(alpha = opacity.coerceIn(0.1f, 1f)),
                        maxLines = 1,
                        modifier = Modifier.rotate(if (diagonal) -25f else 0f)
                    )
                }
            }
            SectionCard("3. Style") {
                Text("Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(colorNames, colorIdx) { colorIdx = it }
                Spacer(Modifier.height(8.dp))
                Text("Size", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf("Small", "Medium", "Large"), sizeIdx) { sizeIdx = it }
                Spacer(Modifier.height(8.dp))
                Text("Layout", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf("Diagonal", "Horizontal"), if (diagonal) 0 else 1) { diagonal = it == 0 }
                Spacer(Modifier.height(8.dp))
                Text("Opacity: ${(opacity * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.1f..0.8f)
            }
            NameField(outName, { outName = it })
        }
    }
}

// =====================================================================
// 7. ROTATE
// =====================================================================

@Composable
fun PdfRotateScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var angleIdx by remember { mutableStateOf(0) }
    var allPages by remember { mutableStateOf(true) }
    var rangeText by remember { mutableStateOf("") }
    var outName by remember(source) { mutableStateOf("${source?.baseTitle ?: "Document"}_rotated") }

    val angles = listOf(90, -90, 180)
    val total = source?.pageCount ?: 0
    val parsed = if (!allPages && source != null) PdfToolsEngine.parseRanges(rangeText, total) else null
    val rangeProblem = if (parsed != null && rangeText.isNotBlank()) parsed.error else null
    val rangeReady = allPages || (parsed != null && parsed.error == null && parsed.groups.isNotEmpty())

    PdfToolScaffold(
        title = "Rotate Pages",
        description = "Fix sideways or upside-down pages",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF6366F1),
        runner = runner,
        actionLabel = "Rotate & Save",
        actionEnabled = source != null && rangeReady && outName.isNotBlank(),
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            val pages: List<Int>? = if (allPages) null else PdfToolsEngine.flatPages(parsed?.groups ?: emptyList())
            runner.run("Rotating pages...") { _ ->
                PdfToolsEngine.rotate(context, s.file, pages, angles[angleIdx], outName)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        if (source != null) {
            SectionCard("2. Rotation") {
                ChipRow(listOf("90° right", "90° left", "180°"), angleIdx) { angleIdx = it }
                Spacer(Modifier.height(12.dp))
                ChipRow(listOf("All pages", "Selected pages"), if (allPages) 0 else 1) { allPages = it == 0 }
                if (!allPages) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rangeText,
                        onValueChange = { rangeText = it },
                        label = { Text("Pages, e.g. 2, 4-6") },
                        singleLine = true,
                        isError = rangeProblem != null,
                        supportingText = { Text(rangeProblem ?: "This PDF has $total pages.") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            NameField(outName, { outName = it })
        }
    }
}

// =====================================================================
// 8. DELETE PAGES  (thumbnail grid, 3 per row)
// =====================================================================

private fun LazyListScope.pageGridRows(
    total: Int,
    file: File,
    isMarked: (Int) -> Boolean,
    markColor: Color,
    onToggle: (Int) -> Unit
) {
    items((1..total).toList().chunked(3), key = { it.first() }) { rowPages ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowPages.forEach { page ->
                val marked = isMarked(page)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.72f)
                        .clickable { onToggle(page) }
                ) {
                    PdfPageThumb(file, page - 1, Modifier.fillMaxSize())
                    if (marked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(markColor.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Text(
                        "$page",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            repeat(3 - rowPages.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun PdfDeletePagesScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    val selected = remember { mutableStateListOf<Int>() }
    val total = source?.pageCount ?: 0
    val keep = (1..total).filter { it !in selected }

    PdfToolScaffold(
        title = "Delete Pages",
        description = "Tap the pages you want to remove",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFFEF4444),
        runner = runner,
        actionLabel = when {
            source == null -> "Choose a PDF first"
            selected.isEmpty() -> "Tap pages to delete"
            keep.isEmpty() -> "Keep at least 1 page"
            else -> "Delete ${selected.size} page(s)"
        },
        actionEnabled = source != null && selected.isNotEmpty() && keep.isNotEmpty(),
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            val remaining = keep
            runner.run("Removing pages...") { progress ->
                PdfToolsEngine.buildFromPages(context, s.file, listOf(remaining), "${s.baseTitle}_edited", progress)
            }
        },
        ownScroll = true
    ) { notify ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    PdfSourceCard(source, vaultDocs, { source = it; selected.clear() }, notify)
                }
            }
            val s = source
            if (s != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selected.size} of $total selected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { selected.clear() }, modifier = Modifier.height(40.dp)) { Text("Clear", fontSize = 12.sp) }
                        OutlinedButton(onClick = { selected.clear(); selected.addAll((1..total).filter { it % 2 == 1 }) }, modifier = Modifier.height(40.dp)) { Text("Odd", fontSize = 12.sp) }
                        OutlinedButton(onClick = { selected.clear(); selected.addAll((1..total).filter { it % 2 == 0 }) }, modifier = Modifier.height(40.dp)) { Text("Even", fontSize = 12.sp) }
                    }
                }
                pageGridRows(
                    total = total,
                    file = s.file,
                    isMarked = { it in selected },
                    markColor = Color(0xFFEF4444),
                    onToggle = { p -> if (p in selected) selected.remove(p) else selected.add(p) }
                )
                item { Spacer(Modifier.height(16.dp)) }
            } else {
                item { EmptyHint("Choose a PDF to see its pages.") }
            }
        }
    }
}

// =====================================================================
// 9. REARRANGE PAGES
// =====================================================================

@Composable
fun PdfRearrangeScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    val order = remember { mutableStateListOf<Int>() }

    val total = source?.pageCount ?: 0
    val changed = order.isNotEmpty() && order.toList() != (1..total).toList()

    PdfToolScaffold(
        title = "Rearrange Pages",
        description = "Move pages up or down to reorder",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFF8B5CF6),
        runner = runner,
        actionLabel = if (changed) "Save new order" else "Change the order first",
        actionEnabled = source != null && changed,
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            val newOrder = order.toList()
            runner.run("Reordering pages...") { progress ->
                PdfToolsEngine.buildFromPages(context, s.file, listOf(newOrder), "${s.baseTitle}_reordered", progress)
            }
        },
        ownScroll = true
    ) { notify ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    PdfSourceCard(
                        source,
                        vaultDocs,
                        { newSource ->
                            source = newSource
                            order.clear()
                            if (newSource != null) order.addAll(1..newSource.pageCount)
                        },
                        notify
                    )
                }
            }
            val s = source
            if (s != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { val r = order.toList().reversed(); order.clear(); order.addAll(r) },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { Text("Reverse", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { order.clear(); order.addAll(1..total) },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { Text("Reset", fontSize = 13.sp) }
                    }
                }
                itemsIndexed(order, key = { _, page -> page }) { index, page ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PdfPageThumb(s.file, page - 1, Modifier.width(56.dp).height(78.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Position ${index + 1}", fontWeight = FontWeight.Bold)
                                Text("Original page $page", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val t = order[index]
                                        order[index] = order[index - 1]
                                        order[index - 1] = t
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp)
                            ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                            IconButton(
                                onClick = {
                                    if (index < order.size - 1) {
                                        val t = order[index]
                                        order[index] = order[index + 1]
                                        order[index + 1] = t
                                    }
                                },
                                enabled = index < order.size - 1,
                                modifier = Modifier.size(48.dp)
                            ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            } else {
                item { EmptyHint("Choose a PDF to reorder its pages.") }
            }
        }
    }
}

// =====================================================================
// 10. PDF -> JPG / PNG
// =====================================================================

@Composable
fun PdfToImagesScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val runner = rememberPdfToolRunner(viewModel)
    val vaultDocs = rememberVaultPdfs(viewModel)
    var source by remember { mutableStateOf<PdfSource?>(null) }
    var formatIdx by remember { mutableStateOf(0) }
    var qualityIdx by remember { mutableStateOf(0) }
    var allPages by remember { mutableStateOf(true) }
    var rangeText by remember { mutableStateOf("") }

    val total = source?.pageCount ?: 0
    val parsed = if (!allPages && source != null) PdfToolsEngine.parseRanges(rangeText, total) else null
    val rangeProblem = if (parsed != null && rangeText.isNotBlank()) parsed.error else null
    val rangeReady = allPages || (parsed != null && parsed.error == null && parsed.groups.isNotEmpty())
    val pageCountToExport = if (allPages) total else PdfToolsEngine.flatPages(parsed?.groups ?: emptyList()).size

    PdfToolScaffold(
        title = "PDF to Image",
        description = "Turn PDF pages into JPG or PNG pictures",
        icon = Icons.Default.PictureAsPdf,
        color = Color(0xFFEC4899),
        runner = runner,
        actionLabel = if (source != null && rangeReady) "Convert $pageCountToExport page(s)" else "Choose a PDF first",
        actionEnabled = source != null && rangeReady && pageCountToExport > 0,
        onBack = onBackClicked,
        onOpenDocument = onNavigateToDocPreview,
        onAction = action@{
            val s = source ?: return@action
            val pages: List<Int>? = if (allPages) null else PdfToolsEngine.flatPages(parsed?.groups ?: emptyList())
            val dpi = if (qualityIdx == 0) 150 else 220
            runner.run("Creating images...") { progress ->
                PdfToolsEngine.pdfToImages(context, s.file, pages, formatIdx == 1, dpi, s.baseTitle, progress)
            }
        }
    ) { notify ->
        PdfSourceCard(source, vaultDocs, { source = it }, notify)
        if (source != null) {
            SectionCard("2. Options") {
                Text("Format", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf("JPG (smaller)", "PNG (sharper)"), formatIdx) { formatIdx = it }
                Spacer(Modifier.height(8.dp))
                Text("Quality", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf("Standard", "High (bigger files)"), qualityIdx) { qualityIdx = it }
                Spacer(Modifier.height(8.dp))
                Text("Pages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf("All pages", "Selected pages"), if (allPages) 0 else 1) { allPages = it == 0 }
                if (!allPages) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rangeText,
                        onValueChange = { rangeText = it },
                        label = { Text("Pages, e.g. 1-3, 5") },
                        singleLine = true,
                        isError = rangeProblem != null,
                        supportingText = { Text(rangeProblem ?: "This PDF has $total pages.") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// =====================================================================
// PLACEHOLDER FOR TOOLS THAT ARE NOT BUILT YET
// =====================================================================

@Composable
fun ToolUnavailableScreen(title: String, message: String, onBackClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ToolTopBar(title, "Coming soon", Icons.Default.PictureAsPdf, Color(0xFF64748B), onBackClicked)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBackClicked, modifier = Modifier.height(48.dp)) { Text("Back to tools") }
        }
    }
}
