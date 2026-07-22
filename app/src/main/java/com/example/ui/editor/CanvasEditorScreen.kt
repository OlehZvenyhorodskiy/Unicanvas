package com.example.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.core.content.FileProvider
import com.example.audio.RecordingStatus
import com.example.data.models.EraserMode
import com.example.data.models.ToolType
import com.example.ui.components.BottomLeftOverlay
import com.example.ui.components.CanvasTopMenuBottomSheet
import com.example.ui.components.ColorPickerBottomSheet
import com.example.ui.components.GeminiChatBottomSheet
import com.example.ui.components.InsertMenuBottomSheet
import com.example.ui.components.MiniSlidersOverlay
import com.example.ui.components.PageStripBottomSheet
import com.example.ui.components.RightSideToolPanel
import com.example.ui.components.RulerOverlayComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasEditorScreen(
    viewModel: CanvasEditorViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val canvas by viewModel.canvas.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val eraserMode by viewModel.eraserMode.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val strokeOpacity by viewModel.strokeOpacity.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val drawWithFingers by viewModel.drawWithFingers.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val rulerState by viewModel.rulerState.collectAsState()
    val audioStatus by viewModel.audioStatus.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Bottom sheets state
    var showTopMenuSheet by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showInsertSheet by remember { mutableStateOf(false) }
    var showPageStripSheet by remember { mutableStateOf(false) }
    var showGeminiSheet by remember { mutableStateOf(false) }
    var showMiniSliders by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputVal by remember { mutableStateOf("") }

    // Image Picker Launcher
    val insertImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.insertImage(it) }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording()
            Toast.makeText(context, "Запис аудіо розпочато...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Потрібен дозвіл на запис аудіо", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = canvas?.title ?: "Канва",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Audio Recorder Button
                    val isRecording = audioStatus is RecordingStatus.Recording
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAudioRecording()
                                Toast.makeText(context, "Запис лекції збережено!", Toast.LENGTH_SHORT).show()
                            } else {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startAudioRecording()
                                    Toast.makeText(context, "Запис аудіо розпочато...", Toast.LENGTH_SHORT).show()
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Запис аудіо",
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Insert Menu (+)
                    IconButton(onClick = { showInsertSheet = true }) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Вставити", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Export Share Button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Експорт")
                    }

                    // Three Dots Top Menu
                    IconButton(onClick = { showTopMenuSheet = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Налаштування сторінки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        floatingActionButton = {
            // AI Assistant FAB
            FloatingActionButton(
                onClick = { showGeminiSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI-асистент")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Interactive Canvas
            InteractiveCanvas(
                canvasEntity = canvas,
                pageEntity = viewModel.currentPage,
                currentTool = currentTool,
                strokeWidth = strokeWidth,
                strokeOpacity = strokeOpacity,
                currentColor = currentColor,
                drawWithFingers = drawWithFingers,
                rulerState = rulerState,
                zoomScale = zoomScale,
                onZoomChanged = { viewModel.setZoomScale(it) },
                onStrokeAdded = { stroke -> viewModel.addStrokeToCurrentPage(stroke) },
                onEraseAtPoint = { pt, radius -> viewModel.eraseAtPoint(pt, radius) },
                onTwoFingerTap = { viewModel.undo() },
                onMoveShape = { id, x, y -> viewModel.updateShapePosition(id, x, y) },
                onMoveText = { id, x, y -> viewModel.updateTextPosition(id, x, y) },
                onMoveImage = { id, x, y -> viewModel.updateImagePosition(id, x, y) },
                onMoveChart = { id, x, y -> viewModel.updateChartPosition(id, x, y) }
            )

            // 2. Ruler Overlay
            RulerOverlayComponent(
                rulerState = rulerState,
                onRulerChange = { viewModel.setRulerState(it) },
                onCloseClick = { viewModel.setRulerState(rulerState.copy(isVisible = false)) }
            )

            // 3. Top Floating Drawing Toolbar
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tool 1: Pen
                    ToolIconButton(
                        icon = Icons.Default.Create,
                        label = "Ручка",
                        isSelected = currentTool == ToolType.PEN,
                        onClick = { viewModel.selectTool(ToolType.PEN) }
                    )

                    // Tool 2: Pencil
                    ToolIconButton(
                        icon = Icons.Default.Brush,
                        label = "Олівець",
                        isSelected = currentTool == ToolType.PENCIL,
                        onClick = { viewModel.selectTool(ToolType.PENCIL) }
                    )

                    // Tool 3: Ink Pen
                    ToolIconButton(
                        icon = Icons.Default.FormatPaint,
                        label = "Перо",
                        isSelected = currentTool == ToolType.INK_PEN,
                        onClick = { viewModel.selectTool(ToolType.INK_PEN) }
                    )

                    // Tool 4: Selector / Lasso
                    ToolIconButton(
                        icon = Icons.Default.CropSquare,
                        label = "Ласо",
                        isSelected = currentTool == ToolType.SELECTOR,
                        onClick = { viewModel.selectTool(ToolType.SELECTOR) }
                    )

                    // Tool 5: Eraser
                    ToolIconButton(
                        icon = Icons.Default.Radio,
                        label = if (eraserMode == EraserMode.OBJECT) "Стерка (Об'єкт)" else "Стерка (Піксель)",
                        isSelected = currentTool == ToolType.ERASER,
                        onClick = {
                            if (currentTool == ToolType.ERASER) {
                                // Toggle mode
                                val nextMode = if (eraserMode == EraserMode.OBJECT) EraserMode.PIXEL else EraserMode.OBJECT
                                viewModel.setEraserMode(nextMode)
                            } else {
                                viewModel.selectTool(ToolType.ERASER)
                            }
                        }
                    )

                    // Tool 6: Ruler
                    ToolIconButton(
                        icon = Icons.Default.Straighten,
                        label = "Лінійка",
                        isSelected = rulerState.isVisible,
                        onClick = { viewModel.selectTool(ToolType.RULER) }
                    )

                    // Color Picker Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentColor.toColor())
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showColorPickerSheet = true }
                    )

                    // Mini Sliders Toggle Button
                    IconButton(onClick = { showMiniSliders = !showMiniSliders }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Товщина/Прозорість")
                    }
                }
            }

            // 4. Right Side Floating Tool Panel (Thickness, Opacity, Colors)
            RightSideToolPanel(
                strokeWidth = strokeWidth,
                strokeOpacity = strokeOpacity,
                currentColor = currentColor,
                recentColors = recentColors,
                onWidthChange = { viewModel.setStrokeWidth(it) },
                onOpacityChange = { viewModel.setStrokeOpacity(it) },
                onColorSelect = { viewModel.setColor(it) },
                onOpenFullColorPicker = { showColorPickerSheet = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )

            // 5. Mini Sliders Overlay (Optional top overlay)
            AnimatedVisibility(
                visible = showMiniSliders,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                MiniSlidersOverlay(
                    width = strokeWidth,
                    opacity = strokeOpacity,
                    currentColor = currentColor,
                    onWidthChange = { viewModel.setStrokeWidth(it) },
                    onOpacityChange = { viewModel.setStrokeOpacity(it) }
                )
            }

            // 5. Bottom Left Overlay (Page count & Zoom)
            BottomLeftOverlay(
                currentPage = currentPageIndex,
                totalPages = pages.size,
                zoomPercentage = (zoomScale * 100).toInt(),
                onPageIndicatorClick = { showPageStripSheet = true },
                onZoomIndicatorClick = {
                    val nextZoom = when {
                        zoomScale < 1f -> 1f
                        zoomScale < 2f -> 2f
                        else -> 0.5f
                    }
                    viewModel.setZoomScale(nextZoom)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }
    }

    // Bottom Sheets & Dialogs
    if (showTopMenuSheet) {
        CanvasTopMenuBottomSheet(
            currentBgColor = canvas?.backgroundColor ?: 0xFFFFFFFF.toInt(),
            currentPattern = canvas?.backgroundPattern ?: com.example.data.models.BackgroundPattern.BLANK,
            currentPreset = canvas?.pageSizePreset ?: com.example.data.models.PageSizePreset.UNLIMITED,
            onBgColorChange = { viewModel.updateBackgroundColor(it) },
            onPatternChange = { viewModel.updateBackgroundPattern(it) },
            onPresetChange = { preset, w, h -> viewModel.updatePageSizePreset(preset, w, h) },
            onOpenCustomColorPicker = {
                showTopMenuSheet = false
                showColorPickerSheet = true
            },
            onDismiss = { showTopMenuSheet = false }
        )
    }

    if (showColorPickerSheet) {
        ColorPickerBottomSheet(
            initialColor = currentColor,
            recentColors = recentColors,
            onColorSelected = { viewModel.setColor(it) },
            onDismiss = { showColorPickerSheet = false }
        )
    }

    if (showInsertSheet) {
        InsertMenuBottomSheet(
            drawWithFingers = drawWithFingers,
            onDrawWithFingersChange = { viewModel.setDrawWithFingers(it) },
            onInsertImageClick = { insertImageLauncher.launch("image/*") },
            onInsertTextClick = { showTextInputDialog = true },
            onInsertShapeClick = { shapeType -> viewModel.insertShape(shapeType) },
            onInsertChartClick = { viewModel.insertChart() },
            onPasteContentClick = { viewModel.insertText("Вставлено з буфера") },
            onDismiss = { showInsertSheet = false }
        )
    }

    if (showPageStripSheet) {
        PageStripBottomSheet(
            pages = pages,
            currentPageIndex = currentPageIndex,
            onPageSelected = { viewModel.setCurrentPage(it) },
            onAddPageClick = { viewModel.addNewPage() },
            onDeletePageClick = { viewModel.deletePage(it) },
            onDismiss = { showPageStripSheet = false }
        )
    }

    if (showGeminiSheet) {
        GeminiChatBottomSheet(
            messages = chatMessages,
            isLoading = isAiLoading,
            onSendMessage = { viewModel.sendAiPrompt(it) },
            onDismiss = { showGeminiSheet = false }
        )
    }

    // Text Input Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Вставити текст") },
            text = {
                OutlinedTextField(
                    value = textInputVal,
                    onValueChange = { textInputVal = it },
                    label = { Text("Ваш текст") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (textInputVal.isNotBlank()) {
                        viewModel.insertText(textInputVal.trim())
                        textInputVal = ""
                    }
                    showTextInputDialog = false
                }) {
                    Text("Вставити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Експорт конспекту") },
            text = {
                Column {
                    Text("Виберіть формат експорту та збереження у папку 'MeCanvas Exports':")
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportPdf { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Поділитися PDF"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Експортувати в PDF")
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportImage { file ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Поділитися фото"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Експортувати в PNG (Фото)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
