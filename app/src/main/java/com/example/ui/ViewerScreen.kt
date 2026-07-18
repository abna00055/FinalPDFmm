package com.example.ui

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ViewerScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showJumpDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Collect JS Commands and execute them on WebView
    LaunchedEffect(webViewRef, state.currentPdfPath) {
        webViewRef?.let { webView ->
            viewModel.jsCommandFlow.collect { command ->
                webView.evaluateJavascript(command, null)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
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
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Floating Header bar (Shows file name & back button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBackToDashboard() },
                    modifier = Modifier.testTag("viewer_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = state.currentPdfName ?: "عرض ملف PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Controls layout at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated native Search interface
                AnimatedVisibility(
                    visible = state.isSearchActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    NativeSearchBar(viewModel = viewModel, state = state)
                }

                // Custom native Bottom Control Bar
                NativeBottomControlBar(
                    viewModel = viewModel,
                    state = state,
                    onPageIndicatorClick = { showJumpDialog = true }
                )
            }

            // Jump to page Dialog
            if (showJumpDialog) {
                JumpToPageDialog(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    onDismiss = { showJumpDialog = false },
                    onPageSelected = { selectedPage ->
                        viewModel.sendJsCommand("PDFViewerApplication.pdfViewer.currentPageNumber = $selectedPage")
                        showJumpDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PdfWebView(
    pdfPath: String,
    viewModel: PdfViewModel,
    onWebViewCreated: (WebView) -> Unit,
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
                                            // 1. Setup pagechanging event to call back to Android
                                            PDFViewerApplication.eventBus.on('pagechanging', (e) => {
                                                AndroidBridge.onPageChanged(e.pageNumber, e.pagesCount);
                                            });

                                            // 2. Setup find event listener to call back search counts
                                            PDFViewerApplication.eventBus.on('updatefindcontrolstate', (e) => {
                                                if (e.matchesCount) {
                                                    AndroidBridge.onSearchCountUpdated(e.matchesCount.total, e.matchesCount.current);
                                                } else {
                                                    AndroidBridge.onSearchCountUpdated(0, 0);
                                                }
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
                }, "AndroidBridge")

                // Encode file URL properly
                val encodedFileUrl = Uri.encode("file://$pdfPath")
                val viewerUrl = "file:///android_asset/pdfjs/web/viewer.html?file=$encodedFileUrl"
                loadUrl(viewerUrl)
                onWebViewCreated(this)
            }
        },
        modifier = modifier
    )
}

@Composable
fun NativeBottomControlBar(
    viewModel: PdfViewModel,
    state: PdfUiState,
    onPageIndicatorClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("native_bottom_bar"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search button
            IconButton(
                onClick = {
                    if (state.isSearchActive) viewModel.closeSearch() else viewModel.triggerSearch("")
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (state.isSearchActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث في المستند",
                    tint = if (state.isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Zoom Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { viewModel.triggerZoomOut() }) {
                    Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "تصغير", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                Text(
                    text = "${(state.currentScale * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = { viewModel.triggerZoomIn() }) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "تكبير", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Divider
            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Page Navigation Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.triggerPreviousPage() },
                    enabled = state.currentPage > 1
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft, 
                        contentDescription = "الصفحة السابقة",
                        tint = if (state.currentPage > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Interactive Page Indicator Pill
                Button(
                    onClick = onPageIndicatorClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("page_indicator_pill")
                ) {
                    Text(
                        text = if (state.totalPages > 0) "${state.currentPage} / ${state.totalPages}" else "${state.currentPage}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.triggerNextPage() },
                    enabled = state.currentPage < state.totalPages
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight, 
                        contentDescription = "الصفحة التالية",
                        tint = if (state.currentPage < state.totalPages) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeSearchBar(
    viewModel: PdfViewModel,
    state: PdfUiState
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(state.searchQuery) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("native_search_bar"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { viewModel.closeSearch() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق البحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextField(
                value = textState,
                onValueChange = {
                    textState = it
                    viewModel.triggerSearch(it)
                },
                placeholder = { Text("ابحث عن كلمة...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("search_text_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
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

            // Results count and navigation
            if (state.searchQuery.isNotEmpty()) {
                Text(
                    text = if (state.searchMatchesTotal > 0) "${state.searchMatchActive} من ${state.searchMatchesTotal}" else "0 من 0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = { viewModel.navigateSearchPrev() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "المطابقة السابقة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.navigateSearchNext() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "المطابقة التالية",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }
    val maxPage = if (totalPages > 0) totalPages else 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "الانتقال إلى صفحة",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الصفحة ${sliderValue.roundToInt()} من $maxPage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..maxPage.toFloat(),
                    steps = if (maxPage > 2) maxPage - 2 else 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPageSelected(sliderValue.roundToInt()) }
            ) {
                Text("الانتقال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
