package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PdfDatabase
import com.example.data.RecentPdf
import com.example.data.RecentPdfDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class Screen {
    Welcome,
    Dashboard,
    Viewer
}

enum class DashboardTab {
    Settings,
    Tools,
    Folders,
    Home
}

enum class FileFilter {
    All,
    Favorites,
    Recent
}

data class StorageInfo(
    val totalGb: Float = 0f,
    val availableGb: Float = 0f,
    val usedGb: Float = 0f,
    val usedPercentage: Int = 0
)

data class LocalPdfFile(
    val filePath: String,
    val fileName: String,
    val fileSize: String,
    val folderName: String,
    val lastModified: Long,
    val isFavorite: Boolean = false
)

data class PdfUiState(
    val currentScreen: Screen = Screen.Welcome,
    val currentPdfPath: String? = null,
    val currentPdfName: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val searchQuery: String = "",
    val searchMatchesTotal: Int = 0,
    val searchMatchActive: Int = 0,
    val isSearchActive: Boolean = false,
    val currentScale: Float = 1.0f,
    val screenOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    
    // View Options
    val scrollMode: String = "vertical", // "vertical", "horizontal"
    val snapToPage: Boolean = false,
    val autoHideToolbar: Boolean = false,
    
    // Display Settings
    val readingTheme: String = "light", // "light", "dark", "black", "sepia"
    val isSystemBrightness: Boolean = true,
    val customBrightness: Float = 0.5f,
    val keepScreenOn: Boolean = false,
    
    // Bookmarks and AutoScroll
    val bookmarkedPages: Set<Int> = emptySet(),
    val isAutoScrolling: Boolean = false,
    val autoScrollSpeed: Int = 25, // px/s
    
    // Dashboard & Bottom Nav State
    val selectedTab: DashboardTab = DashboardTab.Home,
    val welcomeCompleted: Boolean = false,
    val dashboardSearchQuery: String = "",
    val isGridView: Boolean = true,
    val selectedFilter: FileFilter = FileFilter.All,
    val starredPdfs: Set<String> = emptySet(),
    val allPdfFiles: List<LocalPdfFile> = emptyList(),
    val showToolsTab: Boolean = true,
    val storageInfo: StorageInfo = StorageInfo()
)

class PdfViewModel(private val recentPdfDao: RecentPdfDao) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    val recentPdfs = recentPdfDao.getAllRecentPdfs()

    // SharedFlow to trigger JS commands in WebView
    private val _jsCommandFlow = MutableSharedFlow<String>()
    val jsCommandFlow: SharedFlow<String> = _jsCommandFlow.asSharedFlow()

    fun loadBookmarks(context: Context, filePath: String) {
        val prefs = context.getSharedPreferences("pdf_reader_bookmarks", Context.MODE_PRIVATE)
        val bookmarkStr = prefs.getString(filePath, "") ?: ""
        val bookmarks = if (bookmarkStr.isEmpty()) {
            emptySet()
        } else {
            bookmarkStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
        _uiState.update { it.copy(bookmarkedPages = bookmarks) }
    }

    fun toggleBookmark(context: Context, pageNumber: Int) {
        val path = _uiState.value.currentPdfPath ?: return
        val currentBookmarks = _uiState.value.bookmarkedPages.toMutableSet()
        if (currentBookmarks.contains(pageNumber)) {
            currentBookmarks.remove(pageNumber)
        } else {
            currentBookmarks.add(pageNumber)
        }
        _uiState.update { it.copy(bookmarkedPages = currentBookmarks) }
        
        val prefs = context.getSharedPreferences("pdf_reader_bookmarks", Context.MODE_PRIVATE)
        prefs.edit().putString(path, currentBookmarks.joinToString(",")).apply()
    }

    fun setScrollMode(mode: String) {
        _uiState.update { it.copy(scrollMode = mode) }
        val jsVal = if (mode == "horizontal") 1 else 0
        sendJsCommand("PDFViewerApplication.pdfViewer.scrollMode = $jsVal")
    }

    fun setSnapToPage(snap: Boolean) {
        _uiState.update { it.copy(snapToPage = snap) }
    }

    fun setAutoHideToolbar(autoHide: Boolean) {
        _uiState.update { it.copy(autoHideToolbar = autoHide) }
    }

    fun setReadingTheme(theme: String) {
        _uiState.update { it.copy(readingTheme = theme) }
        sendJsCommand("applyTheme('$theme')")
    }

    fun setSystemBrightness(isSystem: Boolean) {
        _uiState.update { it.copy(isSystemBrightness = isSystem) }
    }

    fun setCustomBrightness(brightness: Float) {
        _uiState.update { it.copy(customBrightness = brightness, isSystemBrightness = false) }
    }

    fun setKeepScreenOn(keep: Boolean) {
        _uiState.update { it.copy(keepScreenOn = keep) }
    }

    fun startAutoScroll(speedPxPerSecond: Int) {
        _uiState.update { it.copy(isAutoScrolling = true, autoScrollSpeed = speedPxPerSecond) }
        sendJsCommand("startAutoScroll($speedPxPerSecond)")
    }

    fun stopAutoScroll() {
        _uiState.update { it.copy(isAutoScrolling = false) }
        sendJsCommand("stopAutoScroll()")
    }

    fun selectPdf(filePath: String, fileName: String) {
        viewModelScope.launch {
            // Find if already exists, then insert or update
            val existing = recentPdfDao.getPdfByPath(filePath)
            val pdf = RecentPdf(
                id = existing?.id ?: 0,
                filePath = filePath,
                fileName = fileName,
                lastPage = existing?.lastPage ?: 1,
                totalPages = existing?.totalPages ?: 0,
                lastOpened = System.currentTimeMillis()
            )
            recentPdfDao.insertOrUpdate(pdf)

            _uiState.update {
                it.copy(
                    currentScreen = Screen.Viewer,
                    currentPdfPath = filePath,
                    currentPdfName = fileName,
                    currentPage = pdf.lastPage,
                    totalPages = pdf.totalPages,
                    searchQuery = "",
                    isSearchActive = false,
                    searchMatchesTotal = 0,
                    searchMatchActive = 0,
                    currentScale = 1.0f
                )
            }
        }
    }

    fun updatePage(page: Int, total: Int) {
        _uiState.update {
            it.copy(currentPage = page, totalPages = total)
        }
        viewModelScope.launch {
            _uiState.value.currentPdfPath?.let { path ->
                recentPdfDao.updateLastPage(path, page)
                // Also update the total pages in the database if it was 0
                val existing = recentPdfDao.getPdfByPath(path)
                if (existing != null && existing.totalPages != total) {
                    recentPdfDao.insertOrUpdate(existing.copy(totalPages = total))
                }
            }
        }
    }

    fun updateSearchMatches(total: Int, current: Int) {
        _uiState.update {
            it.copy(searchMatchesTotal = total, searchMatchActive = current)
        }
    }

    fun goBackToDashboard() {
        _uiState.update {
            it.copy(currentScreen = Screen.Dashboard)
        }
    }

    fun sendJsCommand(command: String) {
        viewModelScope.launch {
            _jsCommandFlow.emit(command)
        }
    }

    // PDF Navigation commands
    fun triggerNextPage() {
        sendJsCommand("PDFViewerApplication.pdfViewer.nextPage()")
    }

    fun triggerPreviousPage() {
        sendJsCommand("PDFViewerApplication.pdfViewer.previousPage()")
    }

    fun triggerZoomIn() {
        sendJsCommand("PDFViewerApplication.pdfViewer.currentScale += 0.25")
        _uiState.update { it.copy(currentScale = it.currentScale + 0.25f) }
    }

    fun triggerZoomOut() {
        sendJsCommand("PDFViewerApplication.pdfViewer.currentScale -= 0.25")
        _uiState.update { it.copy(currentScale = (it.currentScale - 0.25f).coerceAtLeast(0.25f)) }
    }

    fun triggerSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = true) }
        val jsQuery = query.replace("'", "\\'")
        sendJsCommand(
            """
            PDFViewerApplication.eventBus.dispatch('find', {
                query: '$jsQuery',
                caseSensitive: false,
                entireWord: false,
                highlightAll: true,
                findPrevious: false
            })
            """.trimIndent()
        )
    }

    fun navigateSearchNext() {
        val jsQuery = _uiState.value.searchQuery.replace("'", "\\'")
        sendJsCommand(
            """
            PDFViewerApplication.eventBus.dispatch('find', {
                query: '$jsQuery',
                caseSensitive: false,
                entireWord: false,
                highlightAll: true,
                findPrevious: false,
                findAgain: true
            })
            """.trimIndent()
        )
    }

    fun navigateSearchPrev() {
        val jsQuery = _uiState.value.searchQuery.replace("'", "\\'")
        sendJsCommand(
            """
            PDFViewerApplication.eventBus.dispatch('find', {
                query: '$jsQuery',
                caseSensitive: false,
                entireWord: false,
                highlightAll: true,
                findPrevious: true,
                findAgain: true
            })
            """.trimIndent()
        )
    }

    fun closeSearch() {
        _uiState.update {
            it.copy(isSearchActive = false, searchQuery = "", searchMatchesTotal = 0, searchMatchActive = 0)
        }
        // Dispatch find command with empty string to clear highlights
        sendJsCommand(
            """
            PDFViewerApplication.eventBus.dispatch('find', {
                query: '',
                caseSensitive: false,
                entireWord: false,
                highlightAll: true,
                findPrevious: false
            })
            """.trimIndent()
        )
    }

    fun openSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun setScale(scale: Float) {
        val coerced = scale.coerceIn(0.25f, 4.0f)
        _uiState.update { it.copy(currentScale = coerced) }
        sendJsCommand("window.setScale($coerced)")
    }

    fun updateScaleFromJs(scale: Float) {
        val coerced = scale.coerceIn(0.25f, 4.0f)
        _uiState.update { it.copy(currentScale = coerced) }
    }

    fun setScreenOrientation(context: Context, orientation: Int) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = orientation
        _uiState.update { it.copy(screenOrientation = orientation) }
    }

    fun deleteRecentPdf(filePath: String) {
        viewModelScope.launch {
            recentPdfDao.deletePdf(filePath)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            recentPdfDao.clearHistory()
        }
    }

    // Helper functions for file management
    fun openSamplePdf(context: Context) {
        viewModelScope.launch {
            val sampleFile = File(context.cacheDir, "sample_test.pdf")
            try {
                // Copy the pre-bundled pdf.js test PDF to the cache directory as a sample!
                if (!sampleFile.exists()) {
                    context.assets.open("pdfjs/web/compressed.tracemonkey-pldi-09.pdf").use { input ->
                        sampleFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                selectPdf(sampleFile.absolutePath, "الملف التجريبي (TraceMonkey)")
            } catch (e: Exception) {
                e.printStackTrace()
                // In case it fails because pdf.js is still unzipping, try another fallback or notify
            }
        }
    }

    // New navigation and state management methods
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)
        val completed = prefs.getBoolean("welcome_completed", false)
        val showTools = prefs.getBoolean("show_tools_tab", true)
        
        val starredSet = prefs.getStringSet("starred_pdfs", emptySet()) ?: emptySet()
        
        _uiState.update {
            val nextScreen = if (it.currentScreen == Screen.Viewer) Screen.Viewer
                             else if (completed) Screen.Dashboard
                             else Screen.Welcome
            it.copy(
                welcomeCompleted = completed,
                showToolsTab = showTools,
                starredPdfs = starredSet,
                currentScreen = nextScreen
            )
        }
        
        // Scan for local PDF files and populate cache
        scanFiles(context)
    }
    
    fun completeWelcome(context: Context) {
        val prefs = context.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("welcome_completed", true).apply()
        _uiState.update {
            it.copy(
                welcomeCompleted = true,
                currentScreen = Screen.Dashboard
            )
        }
        scanFiles(context)
    }
    
    fun setTab(tab: DashboardTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
    
    fun setDashboardSearchQuery(query: String) {
        _uiState.update { it.copy(dashboardSearchQuery = query) }
    }
    
    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }
    
    fun setFileFilter(filter: FileFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
    
    fun toggleFavorite(context: Context, filePath: String) {
        val prefs = context.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)
        val currentStarred = _uiState.value.starredPdfs.toMutableSet()
        if (currentStarred.contains(filePath)) {
            currentStarred.remove(filePath)
        } else {
            currentStarred.add(filePath)
        }
        prefs.edit().putStringSet("starred_pdfs", currentStarred).apply()
        _uiState.update { it.copy(starredPdfs = currentStarred) }
        
        // Rescan files to update favorites status
        scanFiles(context)
    }
    
    fun setShowToolsTab(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("show_tools_tab", show).apply()
        _uiState.update { it.copy(showToolsTab = show) }
    }
    
    private fun getStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - availableBytes
            
            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val availableGb = availableBytes / (1024f * 1024f * 1024f)
            val usedGb = usedBytes / (1024f * 1024f * 1024f)
            val usedPercentage = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes.toFloat()) * 100).toInt() else 0
            
            StorageInfo(totalGb, availableGb, usedGb, usedPercentage)
        } catch (e: Exception) {
            StorageInfo(0f, 0f, 0f, 0)
        }
    }

    private fun scanDirRecursively(dir: File, result: MutableList<LocalPdfFile>, maxDepth: Int = 3, currentDepth: Int = 0) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirRecursively(file, result, maxDepth, currentDepth + 1)
                }
            } else if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true)) {
                val isAlreadyAdded = result.any { it.filePath == file.absolutePath }
                if (!isAlreadyAdded) {
                    val size = file.length()
                    val sizeStr = when {
                        size > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", size / (1024f * 1024f))
                        size > 1024 -> "${size / 1024} KB"
                        else -> "$size B"
                    }
                    
                    val folderName = dir.name.ifEmpty { "Documents" }
                    result.add(
                        LocalPdfFile(
                            filePath = file.absolutePath,
                            fileName = file.name.replace(".pdf", "", ignoreCase = true).replace("_", " "),
                            fileSize = sizeStr,
                            folderName = folderName,
                            lastModified = file.lastModified(),
                            isFavorite = false
                        )
                    )
                }
            }
        }
    }

    fun scanFiles(context: Context) {
        viewModelScope.launch {
            val storage = getStorageInfo()
            val filesList = mutableListOf<LocalPdfFile>()
            
            // Check storage permission
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            
            if (hasPermission) {
                // 1. Scan via MediaStore
                val contentResolver = context.contentResolver
                val uri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
                val selectionArgs = arrayOf("application/pdf", "%.pdf")
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                
                try {
                    contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        val nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                        val dateIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                        
                        while (cursor.moveToNext()) {
                            if (dataIndex != -1) {
                                val path = cursor.getString(dataIndex)
                                if (path != null) {
                                    val file = File(path)
                                    if (file.exists()) {
                                        val name = if (nameIndex != -1) cursor.getString(nameIndex) else file.name
                                        val rawSize = if (sizeIndex != -1) cursor.getLong(sizeIndex) else file.length()
                                        val lastMod = if (dateIndex != -1) cursor.getLong(dateIndex) * 1000 else file.lastModified()
                                        
                                        val sizeStr = when {
                                            rawSize > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", rawSize / (1024f * 1024f))
                                            rawSize > 1024 -> "${rawSize / 1024} KB"
                                            else -> "$rawSize B"
                                        }
                                        
                                        val parentName = file.parentFile?.name ?: "Documents"
                                        val isFav = _uiState.value.starredPdfs.contains(path)
                                        
                                        filesList.add(
                                            LocalPdfFile(
                                                filePath = path,
                                                fileName = name.replace(".pdf", "", ignoreCase = true).replace("_", " "),
                                                fileSize = sizeStr,
                                                folderName = parentName,
                                                lastModified = lastMod,
                                                isFavorite = isFav
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 2. Scan standard directories recursively
                val publicDirs = listOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    File("/sdcard/Download"),
                    File("/sdcard/Documents"),
                    File("/sdcard/WhatsApp/Media/WhatsApp Documents"),
                    File("/sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
                )
                
                for (dir in publicDirs) {
                    try {
                        if (dir.exists() && dir.isDirectory) {
                            scanDirRecursively(dir, filesList, maxDepth = 3)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // 3. Scan user's cache directory (for imported files or fallback sample files)
            try {
                val cacheFiles = context.cacheDir.listFiles { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                cacheFiles?.forEach { file ->
                    val isAlreadyAdded = filesList.any { it.filePath == file.absolutePath }
                    if (!isAlreadyAdded) {
                        val isFav = _uiState.value.starredPdfs.contains(file.absolutePath)
                        val size = file.length()
                        val sizeStr = when {
                            size > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", size / (1024f * 1024f))
                            size > 1024 -> "${size / 1024} KB"
                            else -> "$size B"
                        }
                        
                        val isPrebuilt = file.name in listOf(
                            "Hören_&_Sprechen_A2.pdf",
                            "Netzwerk_Neu_A2_Übungsbuch.pdf",
                            "NWn_A2_Glossar_Arabisch.pdf",
                            "B1_Wortschatz.pdf",
                            "CamScanner_2025-11-09.pdf"
                        )
                        val folderName = if (isPrebuilt) "ملفات تجريبية" else "مستندات مستوردة"
                        
                        filesList.add(
                            LocalPdfFile(
                                filePath = file.absolutePath,
                                fileName = file.name.replace(".pdf", "", ignoreCase = true).replace("_", " "),
                                fileSize = sizeStr,
                                folderName = folderName,
                                lastModified = file.lastModified(),
                                isFavorite = isFav
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 4. Fallback: If list is still empty and we do NOT have permission, populate sample files
            if (filesList.isEmpty() && !hasPermission) {
                val prebuiltFiles = listOf(
                    "Hören_&_Sprechen_A2.pdf" to "8.1 MB",
                    "Netzwerk_Neu_A2_Übungsbuch.pdf" to "29.0 MB",
                    "NWn_A2_Glossar_Arabisch.pdf" to "4.9 MB",
                    "B1_Wortschatz.pdf" to "5.0 MB",
                    "CamScanner_2025-11-09.pdf" to "7.5 MB"
                )
                
                for ((name, size) in prebuiltFiles) {
                    val file = File(context.cacheDir, name)
                    if (!file.exists()) {
                        try {
                            context.assets.open("pdfjs/web/compressed.tracemonkey-pldi-09.pdf").use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val isFav = _uiState.value.starredPdfs.contains(file.absolutePath)
                    val folder = when (name) {
                        "CamScanner_2025-11-09.pdf" -> "Download"
                        "Hören_&_Sprechen_A2.pdf" -> "WhatsApp"
                        "Netzwerk_Neu_A2_Übungsbuch.pdf" -> "WhatsApp Business"
                        else -> "ملفات تجريبية"
                    }
                    
                    filesList.add(
                        LocalPdfFile(
                            filePath = file.absolutePath,
                            fileName = name.replace(".pdf", "", ignoreCase = true).replace("_", " "),
                            fileSize = size,
                            folderName = folder,
                            lastModified = file.lastModified(),
                            isFavorite = isFav
                        )
                    )
                }
            }
            
            // 5. Update state
            val finalFiles = filesList.map {
                it.copy(isFavorite = _uiState.value.starredPdfs.contains(it.filePath))
            }
            
            _uiState.update { 
                it.copy(
                    allPdfFiles = finalFiles,
                    storageInfo = storage
                )
            }
        }
    }

    fun copyUriToCache(context: Context, uri: Uri, originalName: String): String? {
        return try {
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val tempFile = File(context.cacheDir, safeName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class PdfViewModelFactory(private val recentPdfDao: RecentPdfDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfViewModel(recentPdfDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
