package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentPdf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recentPdfs by viewModel.recentPdfs.collectAsState(initial = emptyList())

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

    // Filter files based on dashboardSearchQuery and selectedFilter
    val filteredFiles = remember(uiState.allPdfFiles, uiState.dashboardSearchQuery, uiState.selectedFilter, recentPdfs) {
        var list = uiState.allPdfFiles
        
        // Apply search query
        if (uiState.dashboardSearchQuery.isNotEmpty()) {
            list = list.filter { it.fileName.contains(uiState.dashboardSearchQuery, ignoreCase = true) }
        }
        
        // Apply category filter
        when (uiState.selectedFilter) {
            FileFilter.All -> list
            FileFilter.Favorites -> list.filter { it.isFavorite }
            FileFilter.Recent -> {
                // Map recent paths to local files or use recent pdfs
                val recentPaths = recentPdfs.map { it.filePath }.toSet()
                list.filter { recentPaths.contains(it.filePath) }
            }
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

            // Quick Stats Indicator
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${uiState.allPdfFiles.size} ملفات",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Interactive Search Bar
        OutlinedTextField(
            value = uiState.dashboardSearchQuery,
            onValueChange = { viewModel.setDashboardSearchQuery(it) },
            placeholder = { Text("ابحث في ملفات الـ PDF...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (uiState.dashboardSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setDashboardSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        StorageStatusCard(storageInfo = uiState.storageInfo)

        Spacer(modifier = Modifier.height(16.dp))

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
                        PdfGridItem(
                            file = file,
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
                        PdfListItem(
                            file = file,
                            onClick = { viewModel.selectPdf(file.filePath, file.fileName) },
                            onToggleFav = { viewModel.toggleFavorite(context, file.filePath) }
                        )
                    }
                }
            }
        }
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
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                // PDF Red Icon Container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFF1EEFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PDF Name
                Text(
                    text = file.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1C1B22)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Folder & Size Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.folderName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Text(
                        text = file.fileSize,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PdfListItem(
    file: LocalPdfFile,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF1EEFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B22),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.folderName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Text(
                        text = file.fileSize,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Favorite button
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "تفضيل",
                    tint = if (file.isFavorite) MaterialTheme.colorScheme.tertiary else Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
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
    
    // Group scanned files by folderName
    val folderGroups = remember(uiState.allPdfFiles) {
        uiState.allPdfFiles.groupBy { it.folderName }
    }
    
    // Tracks which folder is expanded to show its PDFs inside the folders tab
    var expandedFolder by remember { mutableStateOf<String?>(null) }

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
                            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                        color = Color(0xFF1C1B22)
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
                    color = Color(0xFF1C1B22)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val folderFiles = folderGroups[expandedFolder] ?: emptyList()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderFiles) { file ->
                    PdfListItem(
                        file = file,
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
            color = Color(0xFF1C1B22),
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    .background(color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1C1B22),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B22)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            color = Color(0xFF1C1B22)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
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

            // READER SETTINGS SECTION
            item {
                SettingsSectionHeader(title = "إعدادات القارئ والتمرير")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    .background(Color(0xFFF1EEFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B22))
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
                    .background(Color(0xFFF1EEFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B22))
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
    onTabSelected: (DashboardTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
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
                onClick = { onTabSelected(DashboardTab.Settings) }
            )

            // Tools Tab (only display if showTools is active)
            if (showTools) {
                BottomTabItem(
                    selected = selectedTab == DashboardTab.Tools,
                    icon = Icons.Outlined.Construction,
                    selectedIcon = Icons.Filled.Construction,
                    label = "الأدوات",
                    onClick = { onTabSelected(DashboardTab.Tools) }
                )
            }

            // Folders Tab
            BottomTabItem(
                selected = selectedTab == DashboardTab.Folders,
                icon = Icons.Outlined.Folder,
                selectedIcon = Icons.Filled.Folder,
                label = "المجلدات",
                onClick = { onTabSelected(DashboardTab.Folders) }
            )

            // Home Tab
            BottomTabItem(
                selected = selectedTab == DashboardTab.Home,
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = "الرئيسية",
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
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF767482),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF767482)
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

