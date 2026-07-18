package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintAttributes
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class BottomSheetType {
    None,
    MoreOptions,
    ViewOptions,
    DisplaySettings,
    ZoomSettings,
    JumpToPage,
    DocumentInfo,
    Bookmarks,
    AutoScroll,
    DocumentNavigation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity
    
    var activeSheet by remember { mutableStateOf(BottomSheetType.None) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isBarsVisible by remember { mutableStateOf(true) }

    // Dynamic scale initialization on load
    LaunchedEffect(state.currentPdfPath) {
        state.currentPdfPath?.let { path ->
            viewModel.loadBookmarks(context, path)
        }
    }

    // Active reading duration tracker
    LaunchedEffect(state.currentPdfPath) {
        if (state.currentPdfPath != null) {
            while (true) {
                delay(1000L)
                viewModel.incrementReadingTime(context, 1L)
            }
        }
    }

    // Back button handler
    BackHandler {
        viewModel.goBackToDashboard()
    }

    // Programmatic screen keep-awake
    DisposableEffect(state.keepScreenOn) {
        if (state.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Programmatic screen brightness adjustments
    LaunchedEffect(state.isSystemBrightness, state.customBrightness) {
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = if (state.isSystemBrightness) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                state.customBrightness.coerceIn(0.01f, 1.0f)
            }
            window.attributes = layoutParams
        }
    }

    // Auto-hide control bars after 5 seconds of inactivity
    LaunchedEffect(isBarsVisible, state.autoHideToolbar) {
        if (state.autoHideToolbar && isBarsVisible) {
            kotlinx.coroutines.delay(5000L)
            isBarsVisible = false
        }
    }

    // Collect JS commands and execute them on WebView
    LaunchedEffect(webViewRef, state.currentPdfPath) {
        webViewRef?.let { webView ->
            viewModel.jsCommandFlow.collect { command ->
                webView.evaluateJavascript(command, null)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main WebView rendering PDF.js
            if (state.currentPdfPath != null) {
                PdfWebView(
                    pdfPath = state.currentPdfPath!!,
                    viewModel = viewModel,
                    onWebViewCreated = { webViewRef = it },
                    onSingleTap = { isBarsVisible = !isBarsVisible },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // ALWAYS-ON STATUS BAR DIMMED BACKDROP LAYER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .background(Color.Black.copy(alpha = 0.65f))
                    .align(Alignment.TopCenter)
            )

            // FLOATING TOP BAR WITH STATUS BAR BACKDROP (CAPSULE/DYNAMIC ISLAND STYLE)
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 480.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isSearchActive) {
                            NativeSearchBar(
                                viewModel = viewModel,
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                            )
                        } else {
                            val fileName = state.currentPdfName ?: "عرض ملف PDF"
                            val fileNameFontSize = when {
                                fileName.length > 30 -> 11.sp
                                fileName.length > 18 -> 12.sp
                                else -> 14.sp
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Section: Compact Actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            state.currentPdfPath?.let { path ->
                                                sharePdf(context, path, fileName)
                                            }
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.openSearch() },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "البحث",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { activeSheet = BottomSheetType.DocumentNavigation },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = "تصفح المستند",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Centered Title text
                                Text(
                                    text = fileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fileNameFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                // Right Section: Back Arrow
                                IconButton(
                                    onClick = { viewModel.goBackToDashboard() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("viewer_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING TRIGGER BUBBLE (When toolbars are hidden)
            AnimatedVisibility(
                visible = !isBarsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                FloatingActionButton(
                    onClick = { isBarsVisible = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuOpen,
                        contentDescription = "إظهار شريط التحكم"
                    )
                }
            }

            // CONTROLS & BOTTOM DOCK LAYOUT
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sleek circular-dock style Bottom bar with 6 beautifully labeled items
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("native_bottom_bar"),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        tonalElevation = 10.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val isBookmarked = state.bookmarkedPages.contains(state.currentPage)

                            BottomBarItem(
                                icon = Icons.Default.MenuBook,
                                label = "الصفحات",
                                onClick = { activeSheet = BottomSheetType.DocumentNavigation },
                                modifier = Modifier.weight(1f)
                            )
                            BottomBarItem(
                                icon = if (state.scrollMode == "horizontal") Icons.Default.ViewCarousel else Icons.Default.ViewStream,
                                label = "العرض",
                                onClick = { activeSheet = BottomSheetType.ViewOptions },
                                modifier = Modifier.weight(1f)
                            )
                            BottomBarItem(
                                icon = Icons.Default.ZoomIn,
                                label = "الزووم",
                                onClick = { activeSheet = BottomSheetType.ZoomSettings },
                                modifier = Modifier.weight(1f)
                            )
                            BottomBarItem(
                                icon = Icons.Default.Palette,
                                label = "السمات",
                                onClick = { activeSheet = BottomSheetType.DisplaySettings },
                                modifier = Modifier.weight(1f)
                            )
                            BottomBarItem(
                                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                label = "إشارة",
                                onClick = { viewModel.toggleBookmark(context, state.currentPage) },
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            BottomBarItem(
                                icon = Icons.Default.MoreHoriz,
                                label = "أدوات",
                                onClick = { activeSheet = BottomSheetType.MoreOptions },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ADAPTIVE SHEETS MANAGER (SIDE SHEET FOR LANDSCAPE, BOTTOM SHEET FOR PORTRAIT)
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (activeSheet != BottomSheetType.None) {
                if (isLandscape) {
                    // Dimmed background backdrop to dismiss on click outside
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { activeSheet = BottomSheetType.None }
                    )

                    // Left Side sheet panel
                    AnimatedVisibility(
                        visible = activeSheet != BottomSheetType.None,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(360.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                            )
                            .clickable(enabled = false) { } // Prevent clicks through to background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            // Top close bar for landscape side sheet
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { activeSheet = BottomSheetType.None }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Content area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (activeSheet) {
                                    BottomSheetType.MoreOptions -> MoreOptionsSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onNavigate = { activeSheet = it },
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.ViewOptions -> ViewOptionsSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.DisplaySettings -> DisplaySettingsSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.ZoomSettings -> ZoomSettingsSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.JumpToPage -> JumpToPageSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.DocumentInfo -> DocumentInfoSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.Bookmarks -> BookmarksSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.DocumentNavigation -> DocumentNavigationSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    BottomSheetType.AutoScroll -> AutoScrollSheet(
                                        viewModel = viewModel,
                                        state = state,
                                        onDismiss = { activeSheet = BottomSheetType.None }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                } else {
                    // Portrait bottom sheet
                    ModalBottomSheet(
                        onDismissRequest = { activeSheet = BottomSheetType.None },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        when (activeSheet) {
                            BottomSheetType.MoreOptions -> MoreOptionsSheet(
                                viewModel = viewModel,
                                state = state,
                                        onNavigate = { activeSheet = it },
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.ViewOptions -> ViewOptionsSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.DisplaySettings -> DisplaySettingsSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.ZoomSettings -> ZoomSettingsSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.JumpToPage -> JumpToPageSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.DocumentInfo -> DocumentInfoSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.Bookmarks -> BookmarksSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.DocumentNavigation -> DocumentNavigationSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            BottomSheetType.AutoScroll -> AutoScrollSheet(
                                viewModel = viewModel,
                                state = state,
                                onDismiss = { activeSheet = BottomSheetType.None }
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfWebView(
    pdfPath: String,
    viewModel: PdfViewModel,
    onWebViewCreated: (WebView) -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Script to establish bridge listeners, remove PDF.js toolbar, and sync state
                        val setupScript = """
                            (function() {
                                function checkPDFjs() {
                                    if (typeof PDFViewerApplication !== 'undefined' && PDFViewerApplication.initializedPromise) {
                                        PDFViewerApplication.initializedPromise.then(() => {
                                            // Function to safely report current and total pages
                                            function reportPageStatus() {
                                                try {
                                                    var total = PDFViewerApplication.pagesCount || (PDFViewerApplication.pdfViewer && PDFViewerApplication.pdfViewer.pagesCount) || 0;
                                                    var current = PDFViewerApplication.page || (PDFViewerApplication.pdfViewer && PDFViewerApplication.pdfViewer.currentPageNumber) || 1;
                                                    if (total > 0) {
                                                        AndroidBridge.onPageChanged(current, total);
                                                    }
                                                } catch (err) {
                                                    console.error("Error reporting page status: " + err);
                                                }
                                            }

                                            // 1. Setup pagechanging, pagesinit, pagesloaded event to call back to Android
                                            PDFViewerApplication.eventBus.on('pagechanging', (e) => {
                                                AndroidBridge.onPageChanged(e.pageNumber, e.pagesCount);
                                            });

                                            PDFViewerApplication.eventBus.on('pagesinit', (e) => {
                                                reportPageStatus();
                                            });

                                            PDFViewerApplication.eventBus.on('pagesloaded', (e) => {
                                                reportPageStatus();
                                            });

                                            // Report immediately
                                            reportPageStatus();

                                            // Setup a solid fallback interval to poll page changes every 400ms
                                            setInterval(reportPageStatus, 400);

                                            // 2. Setup find event listeners to call back search counts
                                            function reportSearchMatches(e) {
                                                if (e && e.matchesCount) {
                                                    AndroidBridge.onSearchCountUpdated(e.matchesCount.total, e.matchesCount.current);
                                                } else {
                                                    // Fallback check on findController
                                                    try {
                                                        var fc = PDFViewerApplication.findController;
                                                        if (fc && fc._matchesCount) {
                                                            AndroidBridge.onSearchCountUpdated(fc._matchesCount.total, fc._matchesCount.current);
                                                        } else {
                                                            AndroidBridge.onSearchCountUpdated(0, 0);
                                                        }
                                                    } catch(err) {
                                                        AndroidBridge.onSearchCountUpdated(0, 0);
                                                    }
                                                }
                                            }

                                            PDFViewerApplication.eventBus.on('updatefindcontrolstate', (e) => {
                                                reportSearchMatches(e);
                                            });

                                            PDFViewerApplication.eventBus.on('updatefindmatchescount', (e) => {
                                                reportSearchMatches(e);
                                            });

                                            // 3. Complete hide/vanish of PDF.js official viewer toolbar elements
                                            var style = document.createElement('style');
                                            style.type = 'text/css';
                                            style.innerHTML = `
                                                #toolbarContainer, .toolbar, #sidebarContainer, .findbar, #secondaryToolbar { 
                                                    display: none !important; 
                                                } 
                                                #viewerContainer { 
                                                    top: 0 !important; 
                                                    bottom: 0 !important; 
                                                }
                                            `;
                                            document.head.appendChild(style);

                                            // Define custom helper functions globally
                                            window.applyTheme = function(themeName) {
                                                var container = document.getElementById('viewerContainer');
                                                if (!container) return;
                                                container.style.filter = '';
                                                container.style.backgroundColor = '';
                                                document.body.style.backgroundColor = '';
                                                if (themeName === 'dark') {
                                                    container.style.filter = 'invert(0.9) hue-rotate(180deg)';
                                                    container.style.backgroundColor = '#121212';
                                                    document.body.style.backgroundColor = '#121212';
                                                } else if (themeName === 'black') {
                                                    container.style.filter = 'invert(1) contrast(1.1)';
                                                    container.style.backgroundColor = '#000000';
                                                    document.body.style.backgroundColor = '#000000';
                                                } else if (themeName === 'sepia') {
                                                    container.style.filter = 'sepia(0.55) contrast(0.95) brightness(0.95)';
                                                    container.style.backgroundColor = '#F4ECD8';
                                                    document.body.style.backgroundColor = '#F4ECD8';
                                                } else {
                                                    container.style.backgroundColor = '#F4F4F9';
                                                    document.body.style.backgroundColor = '#F4F4F9';
                                                }
                                            };

                                            window.autoScrollInterval = null;
                                            window.startAutoScroll = function(speed) {
                                                if (window.autoScrollInterval) clearInterval(window.autoScrollInterval);
                                                var container = document.getElementById('viewerContainer');
                                                if (!container) return;
                                                var lastTime = performance.now();
                                                window.autoScrollInterval = setInterval(function() {
                                                    var now = performance.now();
                                                    var delta = (now - lastTime) / 1000;
                                                    lastTime = now;
                                                    container.scrollTop += speed * delta;
                                                }, 16);
                                            };

                                            window.stopAutoScroll = function() {
                                                if (window.autoScrollInterval) {
                                                    clearInterval(window.autoScrollInterval);
                                                    window.autoScrollInterval = null;
                                                }
                                            };

                                            window.setScale = function(scale) {
                                                try {
                                                    if (typeof PDFViewerApplication !== 'undefined' && PDFViewerApplication.pdfViewer) {
                                                        PDFViewerApplication.pdfViewer.currentScale = scale;
                                                    }
                                                } catch (e) {
                                                    console.error("Error in setScale JS: " + e);
                                                }
                                            };

                                            window.addEventListener('resize', function() {
                                                if (typeof PDFViewerApplication !== 'undefined' && PDFViewerApplication.pdfViewer) {
                                                    PDFViewerApplication.pdfViewer.update();
                                                }
                                            });

                                            var initialTouchDist = 0;
                                            var initialScale = 1.0;

                                            document.addEventListener('touchstart', function(e) {
                                                if (e.touches.length === 2) {
                                                    var t1 = e.touches[0];
                                                    var t2 = e.touches[1];
                                                    initialTouchDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
                                                    initialScale = PDFViewerApplication.pdfViewer.currentScale || 1.0;
                                                }
                                            }, { passive: true });

                                            document.addEventListener('touchmove', function(e) {
                                                if (e.touches.length === 2 && initialTouchDist > 0) {
                                                    e.preventDefault();
                                                    var t1 = e.touches[0];
                                                    var t2 = e.touches[1];
                                                    var dist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
                                                    var factor = dist / initialTouchDist;
                                                    var newScale = initialScale * factor;
                                                    newScale = Math.min(Math.max(newScale, 0.25), 4.0);
                                                    window.setScale(newScale);
                                                    AndroidBridge.onScaleChanged(newScale);
                                                }
                                            }, { passive: false });

                                            document.addEventListener('touchend', function(e) {
                                                if (e.touches.length < 2) {
                                                    initialTouchDist = 0;
                                                }
                                            }, { passive: true });

                                            // 5. Setup click listener for single tap bar toggling
                                            document.addEventListener('click', function(e) {
                                                var interactive = e.target.closest('a, button, input, select, textarea, .internalLink');
                                                if (interactive) {
                                                    return;
                                                }
                                                AndroidBridge.onSingleTap();
                                            });

                                            // Apply current UI states
                                            window.applyTheme('${state.readingTheme}');
                                            PDFViewerApplication.pdfViewer.scrollMode = ${if (state.snapToPage) 3 else if (state.scrollMode == "horizontal") 1 else 0};

                                            // 4. Initialise page to saved state
                                            var initialPage = ${state.currentPage};
                                            if (initialPage > 1) {
                                                PDFViewerApplication.pdfViewer.currentPageNumber = initialPage;
                                            }
                                        });
                                    } else {
                                        setTimeout(checkPDFjs, 150);
                                    }
                                }
                                checkPDFjs();
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(setupScript, null)
                    }
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onSingleTap() {
                        coroutineScope.launch {
                            onSingleTap()
                        }
                    }

                    @android.webkit.JavascriptInterface
                    fun onPageChanged(pageNumber: Int, pagesCount: Int) {
                        coroutineScope.launch {
                            viewModel.updatePage(pageNumber, pagesCount)
                        }
                    }

                    @android.webkit.JavascriptInterface
                    fun onSearchCountUpdated(total: Int, current: Int) {
                        coroutineScope.launch {
                            viewModel.updateSearchMatches(total, current)
                        }
                    }

                    @android.webkit.JavascriptInterface
                    fun onScaleChanged(scale: Float) {
                        coroutineScope.launch {
                            viewModel.updateScaleFromJs(scale)
                        }
                    }
                }, "AndroidBridge")

                // Encode file URL properly
                val encodedFileUrl = Uri.encode("file://$pdfPath")
                val currentPage = state.currentPage
                val viewerUrl = "file:///android_asset/pdfjs/web/viewer.html?file=$encodedFileUrl#page=$currentPage"
                loadUrl(viewerUrl)
                onWebViewCreated(this)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeSearchBar(
    viewModel: PdfViewModel,
    state: PdfUiState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(state.searchQuery) }

    // Keep the TextField in sync with the state when state resets (e.g. search closed)
    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery != textState) {
            textState = state.searchQuery
        }
    }

    Surface(
        modifier = modifier
            .testTag("native_search_bar"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { viewModel.closeSearch() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق البحث",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            TextField(
                value = textState,
                onValueChange = {
                    textState = it
                    viewModel.triggerSearch(it)
                },
                placeholder = { Text("ابحث عن كلمة...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("search_text_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    }
                )
            )

            if (state.searchQuery.isNotEmpty()) {
                Text(
                    text = if (state.searchMatchesTotal > 0) "${state.searchMatchActive} من ${state.searchMatchesTotal}" else "0 من 0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.navigateSearchPrev() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "المطابقة السابقة",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { viewModel.navigateSearchNext() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "المطابقة التالية",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ----------------- SUB-SHEETS FOR OPTIONS -----------------

@Composable
fun MoreOptionsSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onNavigate: (BottomSheetType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "خيارات إضافية",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Grid columns
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoreOptionGridItem(
                    icon = Icons.Outlined.Pin,
                    label = "الإشارات المرجعية",
                    onClick = {
                        onNavigate(BottomSheetType.Bookmarks)
                    }
                )
                MoreOptionGridItem(
                    icon = Icons.Outlined.DirectionsRun,
                    label = "التمرير التلقائي",
                    onClick = {
                        onNavigate(BottomSheetType.AutoScroll)
                    }
                )
                MoreOptionGridItem(
                    icon = Icons.Outlined.Print,
                    label = "طباعة المستند",
                    onClick = {
                        onDismiss()
                        state.currentPdfPath?.let { path ->
                            printPdf(context, path, state.currentPdfName ?: "doc.pdf")
                        }
                    }
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoreOptionGridItem(
                    icon = Icons.Outlined.Navigation,
                    label = "الانتقال إلى صفحة",
                    onClick = {
                        onNavigate(BottomSheetType.JumpToPage)
                    }
                )
                MoreOptionGridItem(
                    icon = Icons.Outlined.Info,
                    label = "معلومات المستند",
                    onClick = {
                        onNavigate(BottomSheetType.DocumentInfo)
                    }
                )
                MoreOptionGridItem(
                    icon = Icons.Outlined.Share,
                    label = "مشاركة الملف",
                    onClick = {
                        onDismiss()
                        state.currentPdfPath?.let { path ->
                            sharePdf(context, path, state.currentPdfName ?: "doc.pdf")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MoreOptionGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ViewOptionsSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "خيارات العرض",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 20.dp),
            textAlign = TextAlign.Start
        )

        Text(
            text = "وضع التمرير",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isHorizontal = state.scrollMode == "horizontal"
            
            // Horizontal scroll option
            Surface(
                onClick = { viewModel.setScrollMode("horizontal") },
                shape = RoundedCornerShape(16.dp),
                color = if (isHorizontal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = if (isHorizontal) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewCarousel,
                        contentDescription = "أفقي",
                        tint = if (isHorizontal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أفقي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHorizontal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Vertical scroll option
            Surface(
                onClick = { viewModel.setScrollMode("vertical") },
                shape = RoundedCornerShape(16.dp),
                color = if (!isHorizontal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = if (!isHorizontal) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewStream,
                        contentDescription = "عمودي",
                        tint = if (!isHorizontal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "عمودي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isHorizontal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        // Snap to page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "محاذاة تلقائية إلى الصفحة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تثبيت عرض الصفحة على حواف الشاشة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.snapToPage,
                onCheckedChange = { viewModel.setSnapToPage(it) }
            )
        }

        // Auto-hide controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إخفاء شريط الأدوات تلقائيًا",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "إخفاء عناصر التحكم كليًا بعد ٥ ثوانٍ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.autoHideToolbar,
                onCheckedChange = { viewModel.setAutoHideToolbar(it) }
            )
        }
    }
}

@Composable
fun ZoomSettingsSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "الزووم والتحكم في العرض",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 4.dp),
            textAlign = TextAlign.Start
        )

        Text(
            text = "تعديل مقياس الصفحات واتجاه الشاشة للقراءة المريحة",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp),
            textAlign = TextAlign.Start
        )

        // Zoom Control Row (- Percentage +)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "مقياس الزووم الحالي",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.triggerZoomOut() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "تصغير",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${(state.currentScale * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 48.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { viewModel.triggerZoomIn() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "تكبير",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Zoom Slider
        Slider(
            value = state.currentScale,
            onValueChange = { viewModel.setScale(it) },
            valueRange = 0.25f..3.0f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Quick zoom preset buttons
        Text(
            text = "خيارات ملاءمة الصفحة السريعة",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Actual size
            Button(
                onClick = { viewModel.setScale(1.0f) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.currentScale == 1.0f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (state.currentScale == 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "الحجم الأصلي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Fit width
            Button(
                onClick = { viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentScaleValue = 'page-width'") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "ملائمة العرض", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Fit page
            Button(
                onClick = { viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentScaleValue = 'page-fit'") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "ملائمة الصفحة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Screen orientation options
        Text(
            text = "اتجاه الشاشة المفضل",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val orientations = listOf(
                Triple(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "عمودي (طولي)", Icons.Default.StayCurrentPortrait),
                Triple(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "أفقي (عرضي)", Icons.Default.StayCurrentLandscape),
                Triple(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, "تلقائي (حسب النظام)", Icons.Default.ScreenRotation)
            )

            orientations.forEach { (orientationVal, label, icon) ->
                val isSelected = state.screenOrientation == orientationVal
                Button(
                    onClick = { viewModel.setScreenOrientation(context, orientationVal) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun DisplaySettingsSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "إعدادات العرض ومظهر القراءة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 20.dp),
            textAlign = TextAlign.Start
        )

        // Reading Theme Boxes
        Text(
            text = "مظهر القراءة",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                Triple("light", "فاتح", Color.White),
                Triple("dark", "داكن", Color(0xFF1E1E2E)),
                Triple("black", "أسود", Color.Black),
                Triple("sepia", "ورق دافئ", Color(0xFFF4ECD8))
            )

            themes.forEach { (themeName, label, bgColor) ->
                val isSelected = state.readingTheme == themeName
                val outlineColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                val textColor = if (themeName == "light" || themeName == "sepia") Color.Black else Color.White

                Surface(
                    onClick = { viewModel.setReadingTheme(themeName) },
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, outlineColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "محدد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        // Brightness Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "سطوع الشاشة مخصص",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "تلقائي للنظام",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Checkbox(
                    checked = state.isSystemBrightness,
                    onCheckedChange = { viewModel.setSystemBrightness(it) }
                )
            }
        }

        if (!state.isSystemBrightness) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness5,
                    contentDescription = "منخفض",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value = state.customBrightness,
                    onValueChange = { viewModel.setCustomBrightness(it) },
                    valueRange = 0.05f..1.0f,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Brightness7,
                    contentDescription = "مرتفع",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keep Screen On
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إبقاء الشاشة مفعّلة دائمًا",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "منع إيقاف تشغيل الشاشة تلقائيًا أثناء القراءة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) }
            )
        }
    }
}

@Composable
fun JumpToPageSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val maxPage = if (state.totalPages > 0) state.totalPages else 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "الانتقال السريع إلى صفحة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Text(
            text = "الصفحة الحالية: ${state.currentPage} من $maxPage",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                errorMessage = null
            },
            label = { Text("أدخل رقم الصفحة") },
            placeholder = { Text("مثال: 5") },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    val target = textInput.toIntOrNull()
                    if (target == null || target < 1 || target > maxPage) {
                        errorMessage = "يرجى إدخال رقم صحيح بين ١ و $maxPage"
                    } else {
                        viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $target")
                        onDismiss()
                    }
                }
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء")
            }

            Button(
                onClick = {
                    val target = textInput.toIntOrNull()
                    if (target == null || target < 1 || target > maxPage) {
                        errorMessage = "يرجى إدخال رقم صحيح بين ١ و $maxPage"
                    } else {
                        viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $target")
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("انطلق")
            }
        }
    }
}

@Composable
fun DocumentInfoSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fileSizeStr = getReadableFileSize(state.currentPdfPath)
    
    val formatVersion = "PDF 1.7"
    val securityStr = "مؤمن تلقائياً (غير مشفر)"
    val lastOpenedDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "معلومات المستند",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 20.dp),
            textAlign = TextAlign.Start
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DocInfoRow(label = "اسم الملف", value = state.currentPdfName ?: "غير معروف")
            }
            item {
                DocInfoRow(label = "حجم الملف", value = fileSizeStr)
            }
            item {
                DocInfoRow(label = "العدد الفعلي للصفحات", value = "${state.totalPages} صفحات")
            }
            item {
                DocInfoRow(label = "موقع التخزين", value = state.currentPdfPath ?: "مجلد التطبيق")
            }
            item {
                DocInfoRow(label = "إصدار التنسيق", value = formatVersion)
            }
            item {
                DocInfoRow(label = "الأمان والخصوصية", value = securityStr)
            }
            item {
                DocInfoRow(label = "تاريخ آخر قراءة", value = lastOpenedDateStr)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إغلاق")
        }
    }
}

@Composable
fun DocInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BookmarksSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "الإشارات المرجعية المضافة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Start
        )

        val bookmarks = state.bookmarkedPages.toList().sorted()

        if (bookmarks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "فارغ",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لم يتم إضافة أي إشارات مرجعية بعد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اضغط على أيقونة الشريط في الأسفل لحفظ الصفحة الحالية للرجوع إليها لاحقاً.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks) { page ->
                    val context = LocalContext.current
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $page")
                                        onDismiss()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "علامة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "الصفحة $page",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.toggleBookmark(context, page) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الإشارة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutoScrollSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    var speedState by remember { mutableFloatStateOf(state.autoScrollSpeed.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "التمرير التلقائي الذكي",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Text(
            text = "قراءة هادئة خالية من اليدين بمعدل سرعة مريح لتمرير الصفحات تلقائياً.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Presets speed selection
        Text(
            text = "تحديد سرعة مسبقة",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                Pair("بطيء", 12f),
                Pair("متوسط", 25f),
                Pair("سريع", 55f),
                Pair("سريع جداً", 90f)
            )

            presets.forEach { (label, speed) ->
                val isSelected = speedState == speed
                Surface(
                    onClick = {
                        speedState = speed
                        if (state.isAutoScrolling) {
                            viewModel.startAutoScroll(speed.roundToInt())
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Custom speed slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ضبط دقيق للسرعة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${speedState.roundToInt()} بكسل/ث",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = speedState,
            onValueChange = {
                speedState = it
                if (state.isAutoScrolling) {
                    viewModel.startAutoScroll(it.roundToInt())
                }
            },
            valueRange = 5f..150f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Large Start/Stop toggle button
        Button(
            onClick = {
                if (state.isAutoScrolling) {
                    viewModel.stopAutoScroll()
                } else {
                    viewModel.startAutoScroll(speedState.roundToInt())
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isAutoScrolling) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (state.isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isAutoScrolling) "إيقاف" else "بدء"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isAutoScrolling) "إيقاف التمرير التلقائي" else "بدء التمرير التلقائي",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ----------------- PRINT & SHARE UTILITY INTEGRATIONS -----------------

fun getReadableFileSize(filePath: String?): String {
    if (filePath == null) return "0 KB"
    val file = File(filePath)
    if (!file.exists()) return "0 KB"
    val bytes = file.length()
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1] + ""
    return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

fun sharePdf(context: Context, filePath: String, fileName: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(intent, "مشاركة الملف عبر:"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun printPdf(context: Context, filePath: String, fileName: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${context.packageName} - $fileName"
        val file = File(filePath)
        if (file.exists()) {
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(fileName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        file.inputStream().use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, printAdapter, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun BottomBarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isSelected: Boolean = false
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun PdfPageThumbnail(
    pdfPath: String,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pdfPath, pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(pdfPath, pageIndex) {
        kotlinx.coroutines.Dispatchers.IO.run {
            try {
                val file = File(pdfPath)
                if (file.exists()) {
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    if (pageIndex >= 0 && pageIndex < renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        
                        val width = 180
                        val height = 260
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap = bmp
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "الصفحة ${pageIndex + 1}",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentNavigationSheet(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(2) } // default to Pages tab
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 550.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "تصفح المستند",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        
        val tabTitles = listOf(
            "التفاصيل",
            "الإشارات (${state.bookmarkedPages.size})",
            "الصفحات (${state.totalPages})"
        )
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Details tab
                    val fileSizeStr = getReadableFileSize(state.currentPdfPath)
                    val formatVersion = "PDF 1.7"
                    val securityStr = "مؤمن تلقائياً (غير مشفر)"
                    val lastOpenedDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item { DocInfoRow(label = "اسم الملف", value = state.currentPdfName ?: "غير معروف") }
                        item { DocInfoRow(label = "حجم الملف", value = fileSizeStr) }
                        item { DocInfoRow(label = "العدد الفعلي للصفحات", value = "${state.totalPages} صفحات") }
                        item { DocInfoRow(label = "موقع التخزين", value = state.currentPdfPath ?: "مجلد التطبيق") }
                        item { DocInfoRow(label = "إصدار التنسيق", value = formatVersion) }
                        item { DocInfoRow(label = "الأمان والخصوصية", value = securityStr) }
                        item { DocInfoRow(label = "تاريخ آخر قراءة", value = lastOpenedDateStr) }
                    }
                }
                1 -> {
                    // Bookmarks tab
                    val bookmarks = state.bookmarkedPages.toList().sorted()
                    if (bookmarks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = "فارغ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لم يتم إضافة أي إشارات مرجعية بعد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(bookmarks) { page ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $page")
                                                onDismiss()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "إشارة",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "الصفحة $page",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.toggleBookmark(context, page) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Pages thumbnails grid tab
                    if (state.totalPages <= 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("جاري تحميل الصفحات...")
                        }
                    } else {
                        val lazyGridState = rememberLazyGridState()
                        
                        LaunchedEffect(state.currentPage) {
                            if (state.currentPage in 1..state.totalPages) {
                                try {
                                    lazyGridState.animateScrollToItem(state.currentPage - 1)
                                } catch (e: Exception) {
                                    // Ignore scroll errors
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = lazyGridState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items((1..state.totalPages).toList()) { page ->
                                val isCurrent = page == state.currentPage
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $page")
                                            onDismiss()
                                        }
                                        .padding(6.dp)
                                ) {
                                    state.currentPdfPath?.let { path ->
                                        PdfPageThumbnail(
                                            pdfPath = path,
                                            pageIndex = page - 1,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.7f)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "$page",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
