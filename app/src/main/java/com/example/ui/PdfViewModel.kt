package com.example.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
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

enum class Screen {
    Dashboard,
    Viewer
}

data class PdfUiState(
    val currentScreen: Screen = Screen.Dashboard,
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
    val autoScrollSpeed: Int = 25 // px/s
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
        _uiState.update { it.copy(searchQuery = query, isSearchActive = query.isNotEmpty()) }
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
