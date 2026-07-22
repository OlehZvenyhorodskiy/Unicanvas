package com.example.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.CanvasEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCanvasClick: (String) -> Unit
) {
    val context = LocalContext.current
    val canvases by viewModel.canvases.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Мої канви, 1: Шаблони
    var canvasToRename by remember { mutableStateOf<CanvasEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showAccountMenu by remember { mutableStateOf(false) }

    // File picker for import PDF or photo
    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importPdfOrImage(context, it) { canvasId ->
                onCanvasClick(canvasId)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text("Пошук за назвою...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MeCanvas",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Планшетні нотатки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSearchActive(!isSearchActive) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Пошук")
                        }

                        OutlinedButton(
                            onClick = { importPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Імпорт")
                        }

                        // Account Profile Button
                        Box {
                            IconButton(onClick = { showAccountMenu = true }) {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userName?.take(2)?.uppercase() ?: "ОЗ",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(userName ?: "Олег Звенигородський", fontWeight = FontWeight.Bold)
                                            Text(userEmail ?: "olehzvenyhorodskiy@gmail.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = { showAccountMenu = false }
                                )
                                androidx.compose.material3.HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Вийти з акаунту") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showAccountMenu = false
                                        viewModel.logout()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Мої канви (${canvases.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Шаблони конспектів") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createNewCanvas { canvasId ->
                        onCanvasClick(canvasId)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Створити канву")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // User Google Account Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (userName ?: "Олег Звенигородський").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "ОЗ" },
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName ?: "Олег Звенигородський",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Google",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = userEmail ?: "olehzvenyhorodskiy@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Google Drive Backup Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Локальне збереження та експорт",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Експортовані конспекти у форматі PDF/PNG зберігаються у локальному сховищі та доступні для поширення",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (selectedTabIndex == 0) {
                // My Canvases Tab
                if (canvases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Немає створених канв",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Натисніть '+' або виберіть шаблон, щоб почати малювання та конспектування",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(canvases, key = { it.id }) { canvas ->
                            CanvasCardItem(
                                canvas = canvas,
                                onClick = { onCanvasClick(canvas.id) },
                                onRenameClick = {
                                    canvasToRename = canvas
                                    renameInputText = canvas.title
                                },
                                onDuplicateClick = { viewModel.duplicateCanvas(canvas.id) },
                                onDeleteClick = { viewModel.deleteCanvas(canvas.id) }
                            )
                        }
                    }
                }
            } else {
                // Templates Tab
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TemplateCardItem(
                        title = "Чистий аркуш (Unlimited)",
                        description = "Безкінечний простір для вільних малюнків та конспектів",
                        icon = Icons.Default.Description,
                        onClick = {
                            viewModel.createTemplateCanvas("BLANK") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )

                    TemplateCardItem(
                        title = "Аркуш з графіком (Grid Chart)",
                        description = "Готова заготовка з координатною сіткою та осями X/Y",
                        icon = Icons.Default.GridOn,
                        onClick = {
                            viewModel.createTemplateCanvas("GRID_CHART") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )

                    TemplateCardItem(
                        title = "Конспект Корнелла (Cornell Notes)",
                        description = "Розмітка полів для тез, ключових слів та резюме лекцій",
                        icon = Icons.Default.Edit,
                        onClick = {
                            viewModel.createTemplateCanvas("LECTURE_NOTES") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    canvasToRename?.let { canvas ->
        AlertDialog(
            onDismissRequest = { canvasToRename = null },
            title = { Text("Перейменувати канву") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Назва канви") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInputText.isNotBlank()) {
                        viewModel.renameCanvas(canvas, renameInputText.trim())
                    }
                    canvasToRename = null
                }) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { canvasToRename = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CanvasCardItem(
    canvas: CanvasEntity,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column {
            // Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!canvas.thumbnailPath.isNullOrEmpty() && File(canvas.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(canvas.thumbnailPath),
                        contentDescription = canvas.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = canvas.pageSizePreset.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = canvas.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(Date(canvas.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Опції",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Перейменувати") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Дублювати") },
                            leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicateClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Видалити") },
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
}

@Composable
fun TemplateCardItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
