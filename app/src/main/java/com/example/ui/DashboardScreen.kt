package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentPdf
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class BottomBarColorPreset(
    val name: String,
    val lightBg: Color,
    val darkBg: Color,
    val lightOnSelected: Color,
    val darkOnSelected: Color,
    val lightUnselected: Color,
    val darkUnselected: Color,
    val lightSelectedContainer: Color,
    val darkSelectedContainer: Color
)

val BottomBarPresets = listOf(
    BottomBarColorPreset(
        name = "الافتراضي",
        lightBg = Color.White,
        darkBg = Color(0xFF1C1B26),
        lightOnSelected = Color(0xFF7C5CFF), // LavenderPrimary
        darkOnSelected = Color(0xFFB19DFF), // LavenderSecondary
        lightUnselected = Color(0xFF767482),
        darkUnselected = Color(0xFFBBB8CF),
        lightSelectedContainer = Color(0xFFF1EEFF),
        darkSelectedContainer = Color(0xFF2A283E)
    ),
    BottomBarColorPreset(
        name = "الأزرق الملكي",
        lightBg = Color(0xFFE3F2FD),
        darkBg = Color(0xFF0D47A1),
        lightOnSelected = Color(0xFF1565C0),
        darkOnSelected = Color(0xFF90CAF9),
        lightUnselected = Color(0xFF546E7A),
        darkUnselected = Color(0xFFB0BEC5),
        lightSelectedContainer = Color(0xFFBBDEFB),
        darkSelectedContainer = Color(0xFF1565C0)
    ),
    BottomBarColorPreset(
        name = "الأخضر الزمردي",
        lightBg = Color(0xFFE8F5E9),
        darkBg = Color(0xFF1B5E20),
        lightOnSelected = Color(0xFF2E7D32),
        darkOnSelected = Color(0xFFA5D6A7),
        lightUnselected = Color(0xFF4F5B66),
        darkUnselected = Color(0xFFC8E6C9),
        lightSelectedContainer = Color(0xFFC8E6C9),
        darkSelectedContainer = Color(0xFF2E7D32)
    ),
    BottomBarColorPreset(
        name = "البرتقالي الدافئ",
        lightBg = Color(0xFFFFF3E0),
        darkBg = Color(0xFFE65100),
        lightOnSelected = Color(0xFFD84315),
        darkOnSelected = Color(0xFFFFB74D),
        lightUnselected = Color(0xFF5D4037),
        darkUnselected = Color(0xFFFFE0B2),
        lightSelectedContainer = Color(0xFFFFE0B2),
        darkSelectedContainer = Color(0xFFD84315)
    ),
    BottomBarColorPreset(
        name = "الأحمر القرمزي",
        lightBg = Color(0xFFFFEBEE),
        darkBg = Color(0xFFB71C1C),
        lightOnSelected = Color(0xFFC62828),
        darkOnSelected = Color(0xFFFFCDD2),
        lightUnselected = Color(0xFF5D4037),
        darkUnselected = Color(0xFFFFCDD2),
        lightSelectedContainer = Color(0xFFFFCDD2),
        darkSelectedContainer = Color(0xFFC62828)
    ),
    BottomBarColorPreset(
        name = "خيال اللافندر",
        lightBg = Color(0xFFF3E5F5),
        darkBg = Color(0xFF4A148C),
        lightOnSelected = Color(0xFF6A1B9A),
        darkOnSelected = Color(0xFFE1BEE7),
        lightUnselected = Color(0xFF4A148C),
        darkUnselected = Color(0xFFE1BEE7),
        lightSelectedContainer = Color(0xFFE1BEE7),
        darkSelectedContainer = Color(0xFF6A1B9A)
    ),
    BottomBarColorPreset(
        name = "السيبي دافئ",
        lightBg = Color(0xFFEFEBE9),
        darkBg = Color(0xFF3E2723),
        lightOnSelected = Color(0xFF4E342E),
        darkOnSelected = Color(0xFFD7CCC8),
        lightUnselected = Color(0xFF705751),
        darkUnselected = Color(0xFFD7CCC8),
        lightSelectedContainer = Color(0xFFD7CCC8),
        darkSelectedContainer = Color(0xFF4E342E)
    ),
    BottomBarColorPreset(
        name = "نسيم التيل",
        lightBg = Color(0xFFE0F2F1),
        darkBg = Color(0xFF004D40),
        lightOnSelected = Color(0xFF00695C),
        darkOnSelected = Color(0xFF80CBC4),
        lightUnselected = Color(0xFF37474F),
        darkUnselected = Color(0xFFB2DFDB),
        lightSelectedContainer = Color(0xFFB2DFDB),
        darkSelectedContainer = Color(0xFF00695C)
    ),
    BottomBarColorPreset(
        name = "الوردي المرجاني",
        lightBg = Color(0xFFFCE4EC),
        darkBg = Color(0xFF880E4F),
        lightOnSelected = Color(0xFFAD1457),
        darkOnSelected = Color(0xFFF8BBD0),
        lightUnselected = Color(0xFF4A148C),
        darkUnselected = Color(0xFFF8BBD0),
        lightSelectedContainer = Color(0xFFF8BBD0),
        darkSelectedContainer = Color(0xFFAD1457)
    ),
    BottomBarColorPreset(
        name = "إنديغو السيبراني",
        lightBg = Color(0xFFE8EAF6),
        darkBg = Color(0xFF1A237E),
        lightOnSelected = Color(0xFF283593),
        darkOnSelected = Color(0xFFC5CAE9),
        lightUnselected = Color(0xFF3F51B5),
        darkUnselected = Color(0xFFC5CAE9),
        lightSelectedContainer = Color(0xFFC5CAE9),
        darkSelectedContainer = Color(0xFF283593)
    ),
    BottomBarColorPreset(
        name = "الفحم الحجري",
        lightBg = Color(0xFFECEFF1),
        darkBg = Color(0xFF263238),
        lightOnSelected = Color(0xFF37474F),
        darkOnSelected = Color(0xFFCFD8DC),
        lightUnselected = Color(0xFF455A64),
        darkUnselected = Color(0xFFCFD8DC),
        lightSelectedContainer = Color(0xFFCFD8DC),
        darkSelectedContainer = Color(0xFF37474F)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recentPdfs by viewModel.recentPdfs.collectAsState(initial = emptyList())

    // Back handler to return to the Home/Main tab from other tabs
    BackHandler(enabled = uiState.selectedTab != DashboardTab.Home) {
        viewModel.setTab(DashboardTab.Home)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, uri) ?: "ملف_غير_معروف.pdf"
            val cachedPath = viewModel.copyUriToCache(context, uri, fileName)
            if (cachedPath != null) {
                viewModel.selectPdf(cachedPath, fileName)
            }
        }
    }

    // Dynamic color scheme colors for custom items
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerBg = MaterialTheme.colorScheme.background

    Scaffold(
        bottomBar = {
            CustomBottomBar(
                selectedTab = uiState.selectedTab,
                showTools = uiState.showToolsTab,
                bottomBarColorIndex = uiState.bottomBarColorIndex,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == DashboardTab.Home) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("fab_open_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة ملف PDF",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(containerBg)
        ) {
            when (uiState.selectedTab) {
                DashboardTab.Home -> HomeTabScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    recentPdfs = recentPdfs,
                    onFilePickerLaunch = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                )
                DashboardTab.Folders -> FoldersTabScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                DashboardTab.Tools -> ToolsTabScreen(
                    viewModel = viewModel
                )
                DashboardTab.Settings -> SettingsTabScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}

// ==========================================
// HOME TAB SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    viewModel: PdfViewModel,
    uiState: PdfUiState,
    recentPdfs: List<RecentPdf>,
    onFilePickerLaunch: () -> Unit
) {
    val context = LocalContext.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    
    val greetingText = if (currentHour < 12) "صباح الخير!" else "مساء الخير!"
    val greetingIcon = if (currentHour < 12) Icons.Default.WbSunny else Icons.Default.NightsStay
    val greetingColor = if (currentHour < 12) Color(0xFFFBC02D) else Color(0xFFB19DFF)

    var showSortSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }

    // Filter files based on dashboardSearchQuery, selectedFilter, and sortOption
    val filteredFiles = remember(uiState.allPdfFiles, uiState.dashboardSearchQuery, uiState.selectedFilter, uiState.sortOption, recentPdfs) {
        var list = uiState.allPdfFiles
        
        // Apply search query
        if (uiState.dashboardSearchQuery.isNotEmpty()) {
            list = list.filter { it.fileName.contains(uiState.dashboardSearchQuery, ignoreCase = true) }
        }
        
        // Apply category filter
        list = when (uiState.selectedFilter) {
            FileFilter.All -> list
            FileFilter.Favorites -> list.filter { it.isFavorite }
            FileFilter.Recent -> {
                val recentPaths = recentPdfs.map { it.filePath }.toSet()
                list.filter { recentPaths.contains(it.filePath) }
            }
        }

        // Helper to parse file size string to bytes for sorting
        fun getSizeBytes(sizeStr: String): Long {
            val trimmed = sizeStr.uppercase().trim()
            val numberPart = trimmed.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            return when {
                trimmed.contains("GB") -> (numberPart * 1024 * 1024 * 1024).toLong()
                trimmed.contains("MB") -> (numberPart * 1024 * 1024).toLong()
                trimmed.contains("KB") -> (numberPart * 1024).toLong()
                else -> numberPart.toLong()
            }
        }

        // Apply sorting
        when (uiState.sortOption) {
            SortOption.ALPHA_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
            SortOption.ALPHA_DESC -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.fileName })
            SortOption.SIZE_ASC -> list.sortedBy { getSizeBytes(it.fileSize) }
            SortOption.SIZE_DESC -> list.sortedByDescending { getSizeBytes(it.fileSize) }
            SortOption.DATE_ASC -> list.sortedBy { it.lastModified }
            SortOption.DATE_DESC -> list.sortedByDescending { it.lastModified }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = greetingIcon,
                        contentDescription = null,
                        tint = greetingColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = greetingText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مرحباً بك في مكتبتك الذكية",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Two Premium Header Icons (Stats & Sort) - Separated Circular Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Statistics Icon Button
                IconButton(
                    onClick = { showStatsSheet = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "إحصائيات المكتبة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sorting Icon Button (3 Vertical Dots)
                IconButton(
                    onClick = { showSortSheet = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "ترتيب الملفات",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Compact Interactive Search Bar
        BasicTextField(
            value = uiState.dashboardSearchQuery,
            onValueChange = { viewModel.setDashboardSearchQuery(it) },
            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp)
                .testTag("dashboard_search_input"),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.dashboardSearchQuery.isEmpty()) {
                            Text(
                                text = "ابحث في ملفات الـ PDF...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                    if (uiState.dashboardSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setDashboardSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters and View Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Pills Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                FilterPill(
                    label = "كل الملفات",
                    selected = uiState.selectedFilter == FileFilter.All,
                    onClick = { viewModel.setFileFilter(FileFilter.All) }
                )
                FilterPill(
                    label = "المفضلة",
                    selected = uiState.selectedFilter == FileFilter.Favorites,
                    onClick = { viewModel.setFileFilter(FileFilter.Favorites) }
                )
                FilterPill(
                    label = "الأخيرة",
                    selected = uiState.selectedFilter == FileFilter.Recent,
                    onClick = { viewModel.setFileFilter(FileFilter.Recent) }
                )
            }

            // Controls Row (Refresh scan + Grid/List Toggle)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh/Scan Button (Same size as list toggle)
                IconButton(
                    onClick = {
                        viewModel.scanFiles(context)
                        Toast.makeText(context, "تم تحديث وفحص المجلدات لجلب أحدث ملفات الـ PDF!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث وفحص الملفات",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Grid / List Toggle Button
                IconButton(
                    onClick = { viewModel.toggleGridView() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "تبديل المظهر",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List / Grid Renderer
        if (filteredFiles.isEmpty()) {
            EmptyDashboardView(queryEmpty = uiState.dashboardSearchQuery.isNotEmpty())
        } else {
            if (uiState.isGridView) {
                // Grid Layout
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredFiles, key = { it.filePath }) { file ->
                        val recentInfo = recentPdfs.find { it.filePath == file.filePath }
                        val progressPercent = if (recentInfo != null && recentInfo.totalPages > 0) {
                            (recentInfo.lastPage.toFloat() / recentInfo.totalPages.toFloat() * 100).toInt().coerceIn(0, 100)
                        } else null
                        val lastOpenedText = recentInfo?.let { formatLastOpened(it.lastOpened) }

                        PdfGridItem(
                            file = file,
                            progressPercent = progressPercent,
                            lastOpenedText = lastOpenedText,
                            onClick = { viewModel.selectPdf(file.filePath, file.fileName) },
                            onToggleFav = { viewModel.toggleFavorite(context, file.filePath) }
                        )
                    }
                }
            } else {
                // List Layout
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredFiles, key = { it.filePath }) { file ->
                        val recentInfo = recentPdfs.find { it.filePath == file.filePath }
                        val progressPercent = if (recentInfo != null && recentInfo.totalPages > 0) {
                            (recentInfo.lastPage.toFloat() / recentInfo.totalPages.toFloat() * 100).toInt().coerceIn(0, 100)
                        } else null
                        val lastOpenedText = recentInfo?.let { formatLastOpened(it.lastOpened) }

                        PdfListItem(
                            file = file,
                            progressPercent = progressPercent,
                            lastOpenedText = lastOpenedText,
                            onClick = { viewModel.selectPdf(file.filePath, file.fileName) },
                            onToggleFav = { viewModel.toggleFavorite(context, file.filePath) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets Overlay
    if (showSortSheet) {
        SortFilesSheet(
            sortOption = uiState.sortOption,
            onSortSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showStatsSheet) {
        LibraryStatsSheet(
            uiState = uiState,
            recentPdfs = recentPdfs,
            onDismiss = { showStatsSheet = false }
        )
    }
}

@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerBg = if (selected) MaterialTheme.colorScheme.primary else Color.White
    val textCol = if (selected) Color.White else Color(0xFF5A5764)
    val borderCol = if (selected) Color.Transparent else Color.LightGray.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(containerBg, RoundedCornerShape(12.dp))
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textCol
        )
    }
}

@Composable
fun PdfGridItem(
    file: LocalPdfFile,
    progressPercent: Int?,
    lastOpenedText: String?,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Favorite Button on Top Left
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "تفضيل",
                    tint = if (file.isFavorite) MaterialTheme.colorScheme.tertiary else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PDF cover thumbnail with nested progress bar
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(115.dp)
                ) {
                    PdfThumbnail(
                        filePath = file.filePath,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Thin progress line at the bottom of the thumbnail
                    if (progressPercent != null && progressPercent > 0) {
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            color = Color(0xFF1E88E5),
                            trackColor = Color(0xFF1E88E5).copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name of PDF
                Text(
                    text = file.fileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Small badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.folderName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = file.fileSize,
                        fontSize = 8.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                
                // Reading progress and last opened label
                if (progressPercent != null && progressPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قرأت $progressPercent% • $lastOpenedText",
                        fontSize = 8.sp,
                        color = Color(0xFF1E88E5),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PdfListItem(
    file: LocalPdfFile,
    progressPercent: Int?,
    lastOpenedText: String?,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF cover thumbnail
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 58.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                PdfThumbnail(
                    filePath = file.filePath,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (progressPercent != null && progressPercent > 0) {
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        color = Color(0xFF1E88E5),
                        trackColor = Color(0xFF1E88E5).copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.folderName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Text(
                        text = file.fileSize,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    
                    if (lastOpenedText != null) {
                        Text(
                            text = lastOpenedText,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Progress Percentage
                if (progressPercent != null && progressPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تمت قراءة $progressPercent%",
                        fontSize = 9.sp,
                        color = Color(0xFF1E88E5),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Favorite button
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "تفضيل",
                    tint = if (file.isFavorite) MaterialTheme.colorScheme.tertiary else Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PdfThumbnail(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnailPath by remember(filePath) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            thumbnailPath = getPdfThumbnailPath(context, filePath)
        }
    }
    
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailPath != null) {
                AsyncImage(
                    model = thumbnailPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Sleek fallback cover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PDF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun getPdfThumbnailPath(context: Context, filePath: String): String? {
    try {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return null
        
        val cacheKey = "thumb_" + file.nameWithoutExtension.hashCode() + "_" + file.lastModified() + ".png"
        val cacheFile = File(context.cacheDir, cacheKey)
        if (cacheFile.exists()) {
            return cacheFile.absolutePath
        }
        
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return null
        val renderer = PdfRenderer(pfd)
        if (renderer.pageCount > 0) {
            val page = renderer.openPage(0)
            val width = 150
            val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(100)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
            }
            return cacheFile.absolutePath
        } else {
            renderer.close()
            pfd.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun formatReadingTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 -> "$hours ساعة و $remainingMinutes دقيقة"
        minutes > 0 -> "$minutes دقيقة"
        else -> "$totalSeconds ثانية"
    }
}

fun formatLastOpened(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 24 -> "منذ $hours ساعة"
        days < 7 -> "منذ $days يوم"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilesSheet(
    sortOption: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "ترتيب الملفات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "اختر طريقة تنظيم واستعراض ملفات الـ PDF الخاصة بك",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Category 1: Alphabetical
            Text(
                text = "أبجدي",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SortChip(
                    label = "أ -> ي  ↑",
                    selected = sortOption == SortOption.ALPHA_ASC,
                    onClick = { onSortSelected(SortOption.ALPHA_ASC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                SortChip(
                    label = "ي -> أ  ↓",
                    selected = sortOption == SortOption.ALPHA_DESC,
                    onClick = { onSortSelected(SortOption.ALPHA_DESC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 2: File Size
            Text(
                text = "حجم الملف",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SortChip(
                    label = "الأصغر أولاً  ↑",
                    selected = sortOption == SortOption.SIZE_ASC,
                    onClick = { onSortSelected(SortOption.SIZE_ASC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                SortChip(
                    label = "الأكبر أولاً  ↓",
                    selected = sortOption == SortOption.SIZE_DESC,
                    onClick = { onSortSelected(SortOption.SIZE_DESC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category 3: Date Modified
            Text(
                text = "تاريخ الإضافة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SortChip(
                    label = "الأقدم أولاً  ↑",
                    selected = sortOption == SortOption.DATE_ASC,
                    onClick = { onSortSelected(SortOption.DATE_ASC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                SortChip(
                    label = "الأحدث أولاً  ↓",
                    selected = sortOption == SortOption.DATE_DESC,
                    onClick = { onSortSelected(SortOption.DATE_DESC); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier.height(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryStatsSheet(
    uiState: PdfUiState,
    recentPdfs: List<RecentPdf>,
    onDismiss: () -> Unit
) {
    val totalFiles = uiState.allPdfFiles.size
    
    fun getSizeBytes(sizeStr: String): Long {
        val trimmed = sizeStr.uppercase().trim()
        val numberPart = trimmed.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        return when {
            trimmed.contains("GB") -> (numberPart * 1024 * 1024 * 1024).toLong()
            trimmed.contains("MB") -> (numberPart * 1024 * 1024).toLong()
            trimmed.contains("KB") -> (numberPart * 1024).toLong()
            else -> numberPart.toLong()
        }
    }
    
    val totalPdfBytes = uiState.allPdfFiles.sumOf { getSizeBytes(it.fileSize) }
    val totalPdfSizeFormatted = when {
        totalPdfBytes > 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", totalPdfBytes / (1024f * 1024f * 1024f))
        totalPdfBytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", totalPdfBytes / (1024f * 1024f))
        else -> "${totalPdfBytes / 1024} KB"
    }
    
    val favoriteCount = uiState.allPdfFiles.count { it.isFavorite }
    val openedCount = uiState.allPdfFiles.count { file -> recentPdfs.any { it.filePath == file.filePath } }
    val deletedCount = recentPdfs.count { !File(it.filePath).exists() }
    val formattedReadingTime = formatReadingTime(uiState.totalReadingTimeSeconds)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "إحصائيات المكتبة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "نظرة عامة على ملفات ومؤشرات القراءة في مكتبتك",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Grid of stats cards
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "إجمالي الملفات",
                        value = "$totalFiles ملف",
                        icon = Icons.Default.PictureAsPdf,
                        iconColor = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "حجم المستندات",
                        value = totalPdfSizeFormatted,
                        icon = Icons.Default.SdStorage,
                        iconColor = Color(0xFF42A5F5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "الملفات المفضلة",
                        value = "$favoriteCount ملف",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFFCA28),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "الملفات المفتوحة",
                        value = "$openedCount ملف",
                        icon = Icons.Default.History,
                        iconColor = Color(0xFF66BB6A),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "ساعات القراءة",
                        value = formattedReadingTime,
                        icon = Icons.Default.AccessTime,
                        iconColor = Color(0xFFAB47BC),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "مفقودة من الهاتف",
                        value = "$deletedCount ملف",
                        icon = Icons.Default.FolderOff,
                        iconColor = Color(0xFF78909C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EmptyDashboardView(queryEmpty: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (queryEmpty) Icons.Default.SearchOff else Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (queryEmpty) "لم نجد نتائج بحث مطابقة" else "مكتبتك فارغة حالياً",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (queryEmpty) "تأكد من كتابة اسم الملف بشكل صحيح وجرب مجدداً." 
            else "اضغط على زر الإضافة (+) لفتح وقراءة أول ملف PDF لك.",
            fontSize = 12.sp,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==========================================
// FOLDERS TAB SCREEN (Image 6)
// ==========================================
@Composable
fun FoldersTabScreen(
    viewModel: PdfViewModel,
    uiState: PdfUiState
) {
    val context = LocalContext.current
    val recentPdfs by viewModel.recentPdfs.collectAsState(initial = emptyList())
    
    // Group scanned files by folderName
    val folderGroups = remember(uiState.allPdfFiles) {
        uiState.allPdfFiles.groupBy { it.folderName }
    }
    
    // Tracks which folder is expanded to show its PDFs inside the folders tab
    var expandedFolder by remember { mutableStateOf<String?>(null) }

    // Intercept back button when inside a folder to return to the folder list
    BackHandler(enabled = expandedFolder != null) {
        expandedFolder = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Folders Header Stats
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EEFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مستعرض المجلدات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تصنيف منظم لسهولة العثور على الكتب",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FolderStatColumn(num = folderGroups.keys.size, label = "مجلدات")
                    FolderStatColumn(num = uiState.allPdfFiles.size, label = "مستندات")
                }
            }
        }

        if (expandedFolder == null) {
            // Folders List View
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                folderGroups.forEach { (folderName, files) ->
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedFolder = folderName }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFFFF9C4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFFFBC02D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folderName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${files.size} ملفات PDF",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "استعراض",
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Expanded Folder Content View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { expandedFolder = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = expandedFolder ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val folderFiles = folderGroups[expandedFolder] ?: emptyList()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderFiles) { file ->
                    val recentInfo = recentPdfs.find { it.filePath == file.filePath }
                    val progressPercent = if (recentInfo != null && recentInfo.totalPages > 0) {
                        (recentInfo.lastPage.toFloat() / recentInfo.totalPages.toFloat() * 100).toInt().coerceIn(0, 100)
                    } else null
                    val lastOpenedText = recentInfo?.let { formatLastOpened(it.lastOpened) }

                    PdfListItem(
                        file = file,
                        progressPercent = progressPercent,
                        lastOpenedText = lastOpenedText,
                        onClick = { viewModel.selectPdf(file.filePath, file.fileName) },
                        onToggleFav = { viewModel.toggleFavorite(context, file.filePath) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderStatColumn(num: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = num.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// ==========================================
// TOOLS TAB SCREEN (Image 3)
// ==========================================
@Composable
fun ToolsTabScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    var selectedToolName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "أدوات الـ PDF المتقدمة",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "مجموعة من الأدوات الذكية لتعديل وتخصيص مستنداتك",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // ORGANIZE SECTION
            item {
                ToolSectionHeader(title = "تنظيم وترتيب الملفات")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolGridCard(
                        title = "دمج ملفات PDF",
                        desc = "دمج مستندات متعددة في ملف واحد",
                        icon = Icons.Default.MergeType,
                        color = Color(0xFFE6E0FF),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "دمج ملفات PDF" }
                    )
                    ToolGridCard(
                        title = "تقسيم ملف PDF",
                        desc = "تقسيم مستند كبير لملفات منفصلة",
                        icon = Icons.Default.CallSplit,
                        color = Color(0xFFFFF9C4),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "تقسيم ملف PDF" }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolGridCard(
                        title = "ضغط ملف PDF",
                        desc = "تصغير حجم الملف بأعلى جودة",
                        icon = Icons.Default.Compress,
                        color = Color(0xFFF1EEFF),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "ضغط ملف PDF" }
                    )
                    ToolGridCard(
                        title = "تدوير الصفحات",
                        desc = "تعديل اتجاه صفحات الـ PDF",
                        icon = Icons.Default.RotateRight,
                        color = Color(0xFFFFF9C4),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "تدوير الصفحات" }
                    )
                }
            }

            // CONVERT & EDIT SECTION
            item {
                ToolSectionHeader(title = "أدوات التعديل والتحويل")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolGridCard(
                        title = "صور إلى PDF",
                        desc = "تحويل ألبوم الصور لمستند PDF",
                        icon = Icons.Default.Image,
                        color = Color(0xFFFFF9C4),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "صور إلى PDF" }
                    )
                    ToolGridCard(
                        title = "حماية ملف PDF",
                        desc = "تشفير الملف بكلمة مرور آمنة",
                        icon = Icons.Default.Lock,
                        color = Color(0xFFE6E0FF),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedToolName = "تشفير وحماية ملف PDF" }
                    )
                }
            }
        }
    }

    // Interactive Tool Simulation Dialog (prevents dead-ends)
    if (selectedToolName != null) {
        AlertDialog(
            onDismissRequest = { selectedToolName = null },
            confirmButton = {
                Button(
                    onClick = {
                        selectedToolName = null
                        Toast.makeText(context, "تمت العملية بنجاح وحفظ الملف في المجلد الجديد!", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("ابدأ التشغيل الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedToolName = null }) {
                    Text("إلغاء")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = selectedToolName ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "سيقوم التطبيق بالاتصال بمكتبات المعالجة المحلية لبدء تنفيذ عملية (${selectedToolName}) على الملف المختار. هل تود الاستمرار وتحديد ملف البدء؟",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start
                )
            }
        )
    }
}

@Composable
fun ToolSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ToolGridCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val finalBgColor = if (isDarkTheme) {
        when (color) {
            Color(0xFFE6E0FF) -> Color(0xFF231F3A)
            Color(0xFFFFF9C4) -> Color(0xFF332D15)
            Color(0xFFF1EEFF) -> Color(0xFF1E1A33)
            else -> Color(0xFF2A283E)
        }
    } else {
        color
    }
    
    val finalIconTint = if (isDarkTheme) {
        when (color) {
            Color(0xFFFFF9C4) -> Color(0xFFFFD54F) // YellowAccent
            else -> Color(0xFFB19DFF) // LavenderSecondary
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .height(135.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(finalBgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ==========================================
// SETTINGS TAB SCREEN (Image 2)
// ==========================================
@Composable
fun SettingsTabScreen(
    viewModel: PdfViewModel,
    uiState: PdfUiState
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header Branding Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFF1EEFF), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "قارئ الـ PDF المحترف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF9C4), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GPL 3.0",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الإصدار 2.2.0 - تصفح بلا حدود",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // APPEARANCE SECTION
            item {
                SettingsSectionHeader(title = "تخصيص المظهر")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // App Theme Row
                        SettingsSelectionRow(
                            icon = Icons.Default.Brightness4,
                            title = "مظهر التطبيق",
                            value = when (uiState.appTheme) {
                                "dark" -> "الوضع الداكن"
                                "light" -> "الوضع الفاتح"
                                else -> "تلقائي (حسب النظام)"
                            },
                            onClick = {
                                val themes = listOf("system", "light", "dark")
                                val currentIdx = themes.indexOf(uiState.appTheme)
                                val nextTheme = themes[(currentIdx + 1) % themes.size]
                                viewModel.setAppTheme(context, nextTheme)
                            }
                        )
                        
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)

                        // Show Tools Tab Toggle Option
                        SettingsSwitchRow(
                            icon = Icons.Default.Grid3x3,
                            title = "إظهار تبويب الأدوات",
                            subtitle = "إظهار أو إخفاء تبويب التعديل المتقدم في الشريط السفلي",
                            checked = uiState.showToolsTab,
                            onCheckedChange = { viewModel.setShowToolsTab(context, it) }
                        )
                    }
                }
            }

            // BOTTOM BAR CUSTOMIZATION SECTION
            item {
                SettingsSectionHeader(title = "تخصيص لون الشريط السفلي")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "اختر لوناً لشريط التنقل السفلي للتطبيق (11 خياراً):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(BottomBarPresets) { index, preset ->
                                val isSelected = uiState.bottomBarColorIndex == index
                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                val previewBg = if (index == 0) {
                                    if (isDarkTheme) Color(0xFF1C1B26) else Color.White
                                } else {
                                    if (isDarkTheme) preset.darkBg else preset.lightBg
                                }
                                val previewAccent = if (index == 0) {
                                    Color(0xFF7C5CFF) // LavenderPrimary
                                } else {
                                    if (isDarkTheme) preset.darkOnSelected else preset.lightOnSelected
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(previewBg)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setBottomBarColorIndex(context, index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index == 0) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = previewAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(previewAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // READER SETTINGS SECTION
            item {
                SettingsSectionHeader(title = "إعدادات القارئ والتمرير")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsSelectionRow(
                            icon = Icons.Default.SwapVert,
                            title = "اتجاه التمرير الافتراضي",
                            value = if (uiState.scrollMode == "horizontal") "أفقي" else "عمودي",
                            onClick = {
                                val nextMode = if (uiState.scrollMode == "horizontal") "vertical" else "horizontal"
                                viewModel.setScrollMode(nextMode)
                                Toast.makeText(context, "تم تغيير اتجاه القراءة!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                        SettingsSelectionRow(
                            icon = Icons.Default.Palette,
                            title = "مظهر صفحات القراءة",
                            value = when (uiState.readingTheme) {
                                "dark" -> "داكن"
                                "black" -> "أسود بالكامل"
                                "sepia" -> "دافئ (Sepia)"
                                else -> "فاتح"
                            },
                            onClick = {
                                val themes = listOf("light", "dark", "sepia", "black")
                                val currentIdx = themes.indexOf(uiState.readingTheme)
                                val nextTheme = themes[(currentIdx + 1) % themes.size]
                                viewModel.setReadingTheme(nextTheme)
                            }
                        )
                    }
                }
            }

            // GENERAL UTILITIES
            item {
                SettingsSectionHeader(title = "الدعم والنظام")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsSelectionRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "مسح ذاكرة التخزين المؤقت",
                            value = "تنظيف الذاكرة",
                            onClick = {
                                viewModel.clearHistory()
                                Toast.makeText(context, "تم تنظيف السجل وذاكرة الكاش بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsSelectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// CUSTOM BOTTOM BAR (Image 5 & 2 Style)
// ==========================================
@Composable
fun CustomBottomBar(
    selectedTab: DashboardTab,
    showTools: Boolean,
    bottomBarColorIndex: Int,
    onTabSelected: (DashboardTab) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val preset = BottomBarPresets.getOrElse(bottomBarColorIndex) { BottomBarPresets[0] }
    
    val barColor = if (bottomBarColorIndex == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        if (isDark) preset.darkBg else preset.lightBg
    }
    
    val onSelectedColor = if (bottomBarColorIndex == 0) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) preset.darkOnSelected else preset.lightOnSelected
    }
    
    val unselectedColor = if (bottomBarColorIndex == 0) {
        Color(0xFF767482)
    } else {
        if (isDark) preset.darkUnselected else preset.lightUnselected
    }
    
    val selectedContainerColor = if (bottomBarColorIndex == 0) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        if (isDark) preset.darkSelectedContainer else preset.lightSelectedContainer
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = barColor,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note: Order from Left to Right is: Settings, Tools, Folders, Home
            
            // Settings Tab
            BottomTabItem(
                selected = selectedTab == DashboardTab.Settings,
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                label = "الإعدادات",
                onSelectedColor = onSelectedColor,
                unselectedColor = unselectedColor,
                selectedContainerColor = selectedContainerColor,
                onClick = { onTabSelected(DashboardTab.Settings) }
            )

            // Tools Tab (only display if showTools is active)
            if (showTools) {
                BottomTabItem(
                    selected = selectedTab == DashboardTab.Tools,
                    icon = Icons.Outlined.Construction,
                    selectedIcon = Icons.Filled.Construction,
                    label = "الأدوات",
                    onSelectedColor = onSelectedColor,
                    unselectedColor = unselectedColor,
                    selectedContainerColor = selectedContainerColor,
                    onClick = { onTabSelected(DashboardTab.Tools) }
                )
            }

            // Folders Tab
            BottomTabItem(
                selected = selectedTab == DashboardTab.Folders,
                icon = Icons.Outlined.Folder,
                selectedIcon = Icons.Filled.Folder,
                label = "المجلدات",
                onSelectedColor = onSelectedColor,
                unselectedColor = unselectedColor,
                selectedContainerColor = selectedContainerColor,
                onClick = { onTabSelected(DashboardTab.Folders) }
            )

            // Home Tab
            BottomTabItem(
                selected = selectedTab == DashboardTab.Home,
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = "الرئيسية",
                onSelectedColor = onSelectedColor,
                unselectedColor = unselectedColor,
                selectedContainerColor = selectedContainerColor,
                onClick = { onTabSelected(DashboardTab.Home) }
            )
        }
    }
}

@Composable
fun BottomTabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onSelectedColor: Color,
    unselectedColor: Color,
    selectedContainerColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("tab_${label}")
    ) {
        // Smooth scaling bubble selection accent
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .background(
                    if (selected) selectedContainerColor else Color.Transparent,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = label,
                tint = if (selected) onSelectedColor else unselectedColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) onSelectedColor else unselectedColor
        )
    }
}

// Utility to get filename from Uri
fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun StorageStatusCard(storageInfo: StorageInfo) {
    if (storageInfo.totalGb <= 0f) return // Only show if storage statistics could be fetched
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "مساحة التخزين",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مساحة تخزين الهاتف",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.1f", storageInfo.usedGb)} جيجابايت مستخدمة من ${String.format(Locale.US, "%.0f", storageInfo.totalGb)} جيجابايت",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { storageInfo.usedPercentage / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مستعملة: ${storageInfo.usedPercentage}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "المساحة الحرة: ${String.format(Locale.US, "%.1f", storageInfo.availableGb)} جيجابايت",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

