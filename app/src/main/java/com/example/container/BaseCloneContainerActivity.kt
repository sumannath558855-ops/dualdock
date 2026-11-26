package com.example.container

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.MainActivity
import com.example.R
import com.example.ui.DualDockTab
import com.example.ui.theme.CanvasWhite
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.DualDockTheme
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetInk
import com.example.ui.theme.LavenderTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PastelAmber
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk

abstract class BaseCloneContainerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CLONE_ID = "extra_clone_id"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_TARGET_URL = "extra_target_url"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_BADGE_TEXT = "extra_badge_text"
        const val EXTRA_BADGE_COLOR = "extra_badge_color"
        const val EXTRA_DESKTOP_UA = "extra_desktop_ua"
        const val EXTRA_CLEAR_SESSION = "extra_clear_session"
        const val EXTRA_USE_SESSION_VAULT = "extra_use_session_vault"

        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private const val WEB_SHARE_POLYFILL_JS = """
            (function() {
                if (window.DualDockNativeShare && !navigator.__dualDockSharePatched) {
                    navigator.__dualDockSharePatched = true;
                    navigator.canShare = function() { return true; };
                    navigator.share = function(data) {
                        return new Promise(function(resolve, reject) {
                            try {
                                var title = (data && data.title) ? String(data.title) : document.title;
                                var text = (data && data.text) ? String(data.text) : '';
                                var url = (data && data.url) ? String(data.url) : window.location.href;
                                window.DualDockNativeShare.share(title, text, url);
                                resolve();
                            } catch (e) {
                                reject(e);
                            }
                        });
                    };
                }
            })();
        """
    }

    private inner class WebShareBridge {
        @JavascriptInterface
        fun share(title: String?, text: String?, url: String?) {
            runOnUiThread {
                shareViaSystemSheet(
                    title = title.orEmpty().ifBlank { webViewRef?.title.orEmpty() },
                    text = text.orEmpty(),
                    url = url.orEmpty().ifBlank { webViewRef?.url ?: targetUrlValue }
                )
            }
        }
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var webViewRef: WebView? = null
    private var cloneId: Long = -1L
    private var targetUrlValue: String = ""
    private var useSessionVault: Boolean = false
    private var sessionVault: CloneSessionVault? = null
    private var localStorageRestoredOnce: Boolean = false

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        cloneId = intent.getLongExtra(EXTRA_CLONE_ID, -1L)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty().ifBlank { "DualDock Space" }
        targetUrlValue = normalizeUrl(intent.getStringExtra(EXTRA_TARGET_URL).orEmpty())
        val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        val badgeText = intent.getStringExtra(EXTRA_BADGE_TEXT).orEmpty().ifBlank { "2" }
        val badgeColorHex = intent.getLongExtra(EXTRA_BADGE_COLOR, 0xFF6E56CF)
        val initialDesktopMode = intent.getBooleanExtra(EXTRA_DESKTOP_UA, false)
        val clearOnStart = intent.getBooleanExtra(EXTRA_CLEAR_SESSION, false)
        useSessionVault = intent.getBooleanExtra(EXTRA_USE_SESSION_VAULT, slotIndex == 0)

        if (useSessionVault) {
            sessionVault = CloneSessionVault(applicationContext)
        }

        val slotTag = if (slotIndex in 1..8) "S$slotIndex" else "∞"
        updateRecentsTaskTitle("$displayName • $slotTag")

        setContent {
            DualDockTheme {
                IsolatedContainerScreen(
                    displayName = displayName,
                    initialUrl = targetUrlValue,
                    slotTag = slotTag,
                    badgeText = badgeText,
                    badgeColor = Color(badgeColorHex),
                    initialDesktopMode = initialDesktopMode,
                    clearOnStart = clearOnStart,
                    onClose = { finish() }
                )
            }
        }
    }

    private fun updateRecentsTaskTitle(title: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setTaskDescription(
                    ActivityManager.TaskDescription.Builder()
                        .setLabel(title)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                setTaskDescription(ActivityManager.TaskDescription(title))
            }
        } catch (_: Exception) {
            // Safe ignore on restricted OEMs.
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun IsolatedContainerScreen(
        displayName: String,
        initialUrl: String,
        slotTag: String,
        badgeText: String,
        badgeColor: Color,
        initialDesktopMode: Boolean,
        clearOnStart: Boolean,
        onClose: () -> Unit
    ) {
        var isDesktopMode by remember { mutableStateOf(initialDesktopMode) }
        var pageProgress by remember { mutableIntStateOf(0) }
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var showToolbar by remember { mutableStateOf(true) }
        var showClearDialog by remember { mutableStateOf(false) }
        var showShareDialog by remember { mutableStateOf(false) }
        var dismissOtpBanner by remember { mutableStateOf(false) }
        var webViewRecoveryKey by remember { mutableIntStateOf(0) }
        var currentTitle by remember { mutableStateOf(displayName) }
        var currentUrl by remember { mutableStateOf(initialUrl) }
        val isMyntraLoginPage = currentUrl.contains("myntra.com", ignoreCase = true) &&
            (currentUrl.contains("login", ignoreCase = true) ||
                currentUrl.contains("otp", ignoreCase = true) ||
                currentUrl.contains("auth", ignoreCase = true))

        BackHandler {
            val view = webViewRef
            if (view != null && view.canGoBack()) {
                view.goBack()
            } else {
                onClose()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasWhite)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AnimatedVisibility(
                visible = showToolbar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = PureWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = badgeColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "$slotTag•$badgeText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = JetInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("container_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_web_back),
                                    tint = if (canGoBack) JetInk else MutedInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { webViewRef?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("container_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = stringResource(R.string.cd_web_forward),
                                    tint = if (canGoForward) JetInk else MutedInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    isDesktopMode = !isDesktopMode
                                    webViewRef?.let { wv ->
                                        applyUserAgent(wv, isDesktopMode)
                                        wv.reload()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("container_ua_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDesktopMode) Icons.Rounded.Computer else Icons.Rounded.Smartphone,
                                    contentDescription = stringResource(R.string.cd_web_desktop_toggle),
                                    tint = if (isDesktopMode) DeepLavender else SlateInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { showShareDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("container_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.cd_web_share),
                                    tint = DeepLavender,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("container_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_web_reload),
                                    tint = SlateInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("container_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.cd_web_clear_session),
                                    tint = SlateInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { showToolbar = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("container_collapse_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.cd_web_collapse_bar),
                                    tint = SlateInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("container_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_web_close),
                                    tint = JetInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (pageProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { pageProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = badgeColor,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isMyntraLoginPage && !dismissOtpBanner,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = PastelMint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Need 'Get OTP on WhatsApp'?",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = JetInk
                            )
                            Text(
                                text = "Myntra only shows WhatsApp OTP in its native Android app. Switch to Work Space.",
                                style = MaterialTheme.typography.bodySmall,
                                color = JetInk.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = JetInk,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { openWorkSpaceInMainApp() }
                                .testTag("banner_open_workspace_button")
                        ) {
                            Text(
                                text = "Native →",
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                        IconButton(
                            onClick = { dismissOtpBanner = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss tip",
                                tint = JetInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(webViewRecoveryKey) {
                    val urlToLoad = if (webViewRecoveryKey == 0) initialUrl else currentUrl.ifBlank { initialUrl }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(AndroidColor.WHITE)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    runCatching {
                                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                                    }
                                }

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    allowFileAccess = false
                                    allowContentAccess = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    setSupportMultipleWindows(true)
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                }

                                val cookieManager = runCatching { CookieManager.getInstance() }.getOrNull()
                                runCatching {
                                    cookieManager?.setAcceptCookie(true)
                                    cookieManager?.setAcceptThirdPartyCookies(this, true)
                                }

                                addJavascriptInterface(WebShareBridge(), "DualDockNativeShare")
                                applyUserAgent(this, isDesktopMode)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val uri = request?.url ?: return false
                                        return handleContainerUrl(view, uri)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                        runCatching { view?.evaluateJavascript(WEB_SHARE_POLYFILL_JS, null) }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                        runCatching { view?.evaluateJavascript(WEB_SHARE_POLYFILL_JS, null) }
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        runCatching { CookieManager.getInstance().flush() }
                                        if (useSessionVault && cloneId > 0L) {
                                            val vault = sessionVault
                                            if (!localStorageRestoredOnce) {
                                                localStorageRestoredOnce = true
                                                vault?.restoreLocalStorageIntoWebView(cloneId, view)
                                            }
                                            vault?.saveCookiesForClone(cloneId, url ?: urlToLoad)
                                            vault?.captureLocalStorageFromWebView(cloneId, view)
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        // Cleanly recycle crashed/failed Chromium child renderer process without crashing app.
                                        if (view != null && view == webViewRef) {
                                            webViewRef = null
                                            runCatching {
                                                (view.parent as? ViewGroup)?.removeView(view)
                                                view.destroy()
                                            }
                                            if (webViewRecoveryKey < 3) {
                                                webViewRecoveryKey += 1
                                            }
                                        }
                                        return true
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageProgress = newProgress
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            currentTitle = title
                                        }
                                    }

                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                    ): Boolean {
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                        val mainWebView = view ?: return false
                                        val popupTrampoline = runCatching {
                                            WebView(mainWebView.context).apply {
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(
                                                        popupView: WebView?,
                                                        request: WebResourceRequest?
                                                    ): Boolean {
                                                        val popupUri = request?.url ?: return true
                                                        if (!handleContainerUrl(mainWebView, popupUri)) {
                                                            mainWebView.loadUrl(popupUri.toString())
                                                        }
                                                        runCatching { popupView?.destroy() }
                                                        return true
                                                    }

                                                    override fun onRenderProcessGone(
                                                        popupView: WebView?,
                                                        detail: RenderProcessGoneDetail?
                                                    ): Boolean {
                                                        runCatching { popupView?.destroy() }
                                                        return true
                                                    }
                                                }
                                                postDelayed({ runCatching { destroy() } }, 5_000L)
                                            }
                                        }.getOrNull() ?: return false
                                        transport.webView = popupTrampoline
                                        resultMsg.sendToTarget()
                                        return true
                                    }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    this@BaseCloneContainerActivity.filePathCallback?.onReceiveValue(null)
                                    this@BaseCloneContainerActivity.filePathCallback = filePathCallback
                                    return try {
                                        fileChooserLauncher.launch(arrayOf("*/*"))
                                        true
                                    } catch (_: Exception) {
                                        this@BaseCloneContainerActivity.filePathCallback = null
                                        false
                                    }
                                }
                            }

                            webViewRef = this

                            if (useSessionVault && cloneId > 0L) {
                                sessionVault?.prepareSlotForClone(
                                    cloneId = cloneId,
                                    targetUrl = urlToLoad,
                                    forceClearTarget = clearOnStart && webViewRecoveryKey == 0
                                ) {
                                    post { runCatching { loadUrl(urlToLoad) } }
                                }
                            } else {
                                if (clearOnStart && webViewRecoveryKey == 0) {
                                    runCatching {
                                        cookieManager?.removeAllCookies(null)
                                        cookieManager?.flush()
                                        WebStorage.getInstance().deleteAllData()
                                        clearCache(true)
                                    }
                                }
                                runCatching { loadUrl(urlToLoad) }
                            }
                        }
                    }
                    )
                }

                if (!showToolbar) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        color = JetInk.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 14.dp)
                            .clickable { showToolbar = true }
                            .testTag("container_expand_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = slotTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_web_expand_bar),
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = PureWhite,
                title = {
                    Text(
                        text = "Clear $displayName Session?",
                        style = MaterialTheme.typography.titleLarge,
                        color = JetInk
                    )
                },
                text = {
                    Text(
                        text = "This signs out of $displayName and wipes cookies and storage for this clone only. Your other cloned spaces stay untouched.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateInk
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            if (useSessionVault && cloneId > 0L) {
                                sessionVault?.deleteSession(cloneId)
                            }
                            val cm = CookieManager.getInstance()
                            cm.removeAllCookies { cm.flush() }
                            WebStorage.getInstance().deleteAllData()
                            webViewRef?.clearCache(true)
                            webViewRef?.loadUrl(initialUrl)
                        },
                        modifier = Modifier.testTag("confirm_clear_session_button")
                    ) {
                        Text("Clear Session", color = DangerCoral)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = SlateInk)
                    }
                }
            )
        }
        if (showShareDialog) {
            val activeUrl = webViewRef?.url?.takeIf { it.isNotBlank() } ?: currentUrl
            val activeTitle = currentTitle.ifBlank { displayName }
            val isShoppingWeb = activeUrl.contains("myntra", ignoreCase = true) ||
                activeUrl.contains("flipkart", ignoreCase = true) ||
                activeUrl.contains("amazon", ignoreCase = true) ||
                activeUrl.contains("meesho", ignoreCase = true) ||
                activeUrl.contains("ajio", ignoreCase = true)

            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                containerColor = PureWhite,
                title = {
                    Text(
                        text = "WhatsApp & Share Options",
                        style = MaterialTheme.typography.titleLarge,
                        color = JetInk
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 1-Tap Send directly to WhatsApp
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PastelMint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showShareDialog = false
                                    shareDirectlyToWhatsApp(activeTitle, activeUrl)
                                }
                                .testTag("share_to_whatsapp_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Send on WhatsApp",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = JetInk
                                    )
                                    Text(
                                        text = "Share this product or page straight to WhatsApp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = JetInk.copy(alpha = 0.75f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = JetInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // System Share Sheet
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LavenderTint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showShareDialog = false
                                    shareViaSystemSheet(activeTitle, "", activeUrl)
                                }
                                .testTag("share_via_other_apps_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Share via Other Apps",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = JetInk
                                    )
                                    Text(
                                        text = "Open Android share sheet or copy link",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateInk
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = DeepLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isShoppingWeb) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PastelAmber.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showShareDialog = false
                                        openWorkSpaceInMainApp()
                                    }
                                    .testTag("open_workspace_for_whatsapp_otp_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkOutline,
                                        contentDescription = null,
                                        tint = JetInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Need WhatsApp OTP on $displayName?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = JetInk
                                        )
                                        Text(
                                            text = "Myntra's website defaults to SMS OTP. Tap here to install the native app inside DualDock Work Space for native WhatsApp OTP.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = JetInk.copy(alpha = 0.80f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Close", color = JetInk)
                    }
                }
            )
        }
    }

    private fun handleContainerUrl(view: WebView?, uri: Uri): Boolean {
        val rawUrl = uri.toString()
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val isSelfWhatsAppContainer = targetUrlValue.contains("whatsapp.com", ignoreCase = true)

        // Hand off WhatsApp links (wa.me, api.whatsapp.com, chat.whatsapp.com) to installed WhatsApp
        if ((scheme == "http" || scheme == "https") && !isSelfWhatsAppContainer) {
            val isWhatsAppHandoffHost = host == "wa.me" ||
                host == "api.whatsapp.com" ||
                host == "chat.whatsapp.com"
            if (isWhatsAppHandoffHost) {
                if (launchExternalUri(uri)) {
                    return true
                }
                return false
            }
            return false
        }

        if (scheme == "http" || scheme == "https") {
            return false
        }

        if (scheme == "intent") {
            try {
                val parsedIntent = Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(parsedIntent)
                return true
            } catch (_: Exception) {
                val fallback = extractIntentFallbackUrl(rawUrl)
                if (!fallback.isNullOrBlank()) {
                    view?.loadUrl(fallback)
                }
                return true
            }
        }

        if (scheme == "whatsapp" ||
            scheme == "tel" ||
            scheme == "mailto" ||
            scheme == "sms" ||
            scheme == "upi" ||
            scheme == "tez" ||
            scheme == "phonepe" ||
            scheme == "paytmmp"
        ) {
            if (!launchExternalUri(uri) && scheme == "whatsapp") {
                Toast.makeText(
                    this,
                    "WhatsApp is not installed on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }

        return true
    }

    private fun launchExternalUri(uri: Uri): Boolean {
        return try {
            val externalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(externalIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun shareDirectlyToWhatsApp(title: String, url: String) {
        val shareBody = buildString {
            if (title.isNotBlank()) {
                append(title.trim())
                append("\n")
            }
            append(url.trim())
        }
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in whatsappPackages) {
            try {
                val waIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(pkg)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                startActivity(waIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try next WhatsApp package or fall back below.
            } catch (_: Exception) {
                // Fall back below.
            }
        }
        // Fallback to wa.me deep link or Android share sheet
        val waWebUri = Uri.parse("https://wa.me/?text=${Uri.encode(shareBody)}")
        if (!launchExternalUri(waWebUri)) {
            shareViaSystemSheet(title, "", url)
        }
    }

    private fun shareViaSystemSheet(title: String, text: String, url: String) {
        val shareBody = buildString {
            if (text.isNotBlank()) {
                append(text.trim())
            } else if (title.isNotBlank()) {
                append(title.trim())
            }
            if (url.isNotBlank() && !contains(url)) {
                if (isNotEmpty()) append("\n")
                append(url.trim())
            }
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (title.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        try {
            startActivity(Intent.createChooser(sendIntent, "Share via"))
        } catch (_: Exception) {
            Toast.makeText(this, "No share app available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWorkSpaceInMainApp() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, DualDockTab.WORK_SPACE.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun applyUserAgent(webView: WebView, desktopMode: Boolean) {
        webView.settings.userAgentString = if (desktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
    }

    private fun extractIntentFallbackUrl(intentUri: String): String? {
        val marker = "S.browser_fallback_url="
        val start = intentUri.indexOf(marker)
        if (start < 0) return null
        val after = intentUri.substring(start + marker.length)
        val end = after.indexOf(';')
        val encoded = if (end >= 0) after.substring(0, end) else after
        return Uri.decode(encoded).takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    override fun onResume() {
        super.onResume()
        runCatching { webViewRef?.onResume() }
    }

    override fun onPause() {
        runCatching {
            persistVaultSnapshot()
            webViewRef?.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        val wv = webViewRef
        webViewRef = null
        if (wv != null) {
            runCatching {
                persistVaultSnapshot(wv)
                CookieManager.getInstance().flush()
            }
            runCatching {
                wv.stopLoading()
                wv.removeJavascriptInterface("DualDockNativeShare")
                wv.webViewClient = WebViewClient()
                wv.webChromeClient = null
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.destroy()
            }
        }
        super.onDestroy()
    }

    private fun persistVaultSnapshot(targetWebView: WebView? = webViewRef) {
        if (useSessionVault && cloneId > 0L) {
            val current = runCatching { targetWebView?.url }.getOrNull() ?: targetUrlValue
            sessionVault?.saveCookiesForClone(cloneId, current)
            sessionVault?.captureLocalStorageFromWebView(cloneId, targetWebView)
        }
    }
}
