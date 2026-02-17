package com.udyneos.zashboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.udyneos.zashboard.BuildConfig
import com.udyneos.zashboard.R
import com.udyneos.zashboard.models.ServerConfig
import com.udyneos.zashboard.utils.ClientUtils
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var webView: WebView
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var statusDetail: TextView
    private lateinit var progressBar: View
    private lateinit var nestedScrollView: NestedScrollView  // GANTI ScrollView dengan NestedScrollView
    
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipLocalhost: Chip
    private lateinit var chipAdd: Chip
    
    private lateinit var etHostname: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var etSecret: TextInputEditText
    
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnDisconnect: MaterialButton
    private lateinit var btnOpenBrowser: MaterialButton
    
    private var currentConfig = ServerConfig()
    private var isConnected = false
        set(value) {
            field = value
            runOnUiThread {
                updateButtonStates(value)
                if (!value) {
                    updateConnectionStatus(false, null)
                }
            }
        }
    
    private var currentUrl: String? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val customServers = mutableListOf<Pair<String, Pair<String, Int>>>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupViews()
        setupWebView()
        checkNetworkStatus()
        loadCustomServers()
        setupOnBackPressedDispatcher()
    }
    
    private fun initViews() {
        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Zashboard Client"
        supportActionBar?.subtitle = "v${BuildConfig.VERSION_NAME}"
        
        // Status
        statusIndicator = findViewById(R.id.status_indicator)
        statusText = findViewById(R.id.status_text)
        statusDetail = findViewById(R.id.status_detail)
        
        // WebView
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progress_bar)
        
        // ScrollView - CASTING KE NestedScrollView (sesuai XML)
        nestedScrollView = findViewById(R.id.nested_scroll_view)
        
        // Chip Group
        chipGroup = findViewById(R.id.chip_group)
        chipLocalhost = findViewById(R.id.chip_localhost)
        chipAdd = findViewById(R.id.chip_add)
        
        // Inputs
        etHostname = findViewById(R.id.et_hostname)
        etPort = findViewById(R.id.et_port)
        etSecret = findViewById(R.id.et_secret)
        
        // Buttons
        btnConnect = findViewById(R.id.btn_connect)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        btnOpenBrowser = findViewById(R.id.btn_open_browser)
        
        // Menu
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_cache -> {
                    clearWebViewCache()
                    true
                }
                R.id.action_info -> {
                    showInfoDialog()
                    true
                }
                else -> false
            }
        }
        
        // Initial state
        isConnected = false
        updateConnectionStatus(false, null)
        updateButtonStates(false)
    }
    
    private fun setupViews() {
        // Connect button
        btnConnect.setOnClickListener {
            connectToDashboard()
        }
        
        // Disconnect button - FORCE DISCONNECT
        btnDisconnect.setOnClickListener {
            forceDisconnectFromDashboard()
        }
        
        // Open in browser
        btnOpenBrowser.setOnClickListener {
            openInBrowser()
        }
        
        // Localhost chip
        chipLocalhost.setOnClickListener {
            etHostname.setText("127.0.0.1")
            etPort.setText("9090")
            etSecret.setText("")
            showSnackbar(getString(R.string.set_to_localhost))
        }
        
        chipLocalhost.setOnLongClickListener {
            showServerOptionsDialog("127.0.0.1", 9090, "localhost")
            true
        }
        
        // Add custom server chip
        chipAdd.setOnClickListener {
            showAddServerDialog()
        }
    }
    
    private fun setupWebView() {
        val webSettings = webView.settings
        val currentUserAgent = webSettings.userAgentString
        
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.defaultTextEncodingName = "utf-8"
        webSettings.userAgentString = "$currentUserAgent ZashboardClient/${BuildConfig.VERSION_NAME}"
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                runOnUiThread {
                    progressBar.visibility = View.VISIBLE
                    currentUrl = url
                    updateConnectionStatus(true, getString(R.string.status_connecting))
                }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    isConnected = true
                    currentUrl = url
                    updateConnectionStatus(true, url ?: getString(R.string.status_connected))
                }
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        isConnected = false
                        currentUrl = null
                        updateConnectionStatus(false, getString(R.string.status_failed))
                        showSnackbar("Failed to load: ${error?.description ?: "Unknown error"}")
                    }
                }
            }
            
            override fun onReceivedHttpError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        isConnected = false
                        currentUrl = null
                        updateConnectionStatus(false, "HTTP ${errorResponse?.statusCode}")
                        showSnackbar("Server error: ${errorResponse?.statusCode}")
                    }
                }
            }
        }
        
        // Enable scroll in WebView
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        
        // Enable nested scrolling untuk smooth scroll
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.isNestedScrollingEnabled = true
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        }
    }
    
    private fun connectToDashboard() {
        val hostname = etHostname.text.toString().trim()
        val portText = etPort.text.toString().trim()
        
        if (hostname.isEmpty()) {
            etHostname.error = getString(R.string.hostname_required)
            return
        }
        
        val port = portText.toIntOrNull() ?: 9090
        
        if (!ClientUtils.isNetworkAvailable(this)) {
            showSnackbar(getString(R.string.no_network))
            return
        }
        
        currentConfig = ServerConfig(
            hostname = hostname,
            port = port,
            secret = etSecret.text.toString().trim(),
            disableUpgradeCore = false
        )
        
        val url = "http://$hostname:$port/ui/"
        currentUrl = url
        updateConnectionStatus(true, getString(R.string.status_connecting))
        webView.loadUrl(url)
    }
    
    private fun forceDisconnectFromDashboard() {
        // FORCE DISCONNECT - langsung update UI
        runOnUiThread {
            progressBar.visibility = View.GONE
            
            // FORCE update status ke disconnected
            statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_disconnected)
            statusText.text = getString(R.string.status_disconnected)
            statusDetail.text = "-"
            
            // Update button states
            btnConnect.isEnabled = true
            btnDisconnect.isEnabled = false
            btnOpenBrowser.isEnabled = false
            
            showSnackbar("Disconnected")
        }
        
        // Hentikan semua aktivitas WebView
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        
        // Reset state
        isConnected = false
        currentUrl = null
    }
    
    private fun openInBrowser() {
        if (!isConnected) {
            showSnackbar(getString(R.string.not_connected))
            return
        }
        
        val url = webView.url
        if (url.isNullOrEmpty() || url == "about:blank") {
            showSnackbar("No valid URL to open")
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("Cannot open browser: ${e.message}")
        }
    }
    
    private fun showAddServerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_server, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_server_name)
        val etHost = dialogView.findViewById<TextInputEditText>(R.id.et_server_host)
        val etPort = dialogView.findViewById<TextInputEditText>(R.id.et_server_port)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_server)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = etName.text?.toString()?.trim()
                val host = etHost.text?.toString()?.trim()
                val port = etPort.text?.toString()?.toIntOrNull() ?: 9090
                
                if (!name.isNullOrEmpty() && !host.isNullOrEmpty()) {
                    addCustomServerChip(name, host, port)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showServerOptionsDialog(host: String, port: Int, name: String) {
        val items = arrayOf(
            getString(R.string.option_connect),
            getString(R.string.option_copy),
            getString(R.string.option_remove)
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle("$host:$port")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        etHostname.setText(host)
                        etPort.setText(port.toString())
                        connectToDashboard()
                    }
                    1 -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("server", "$host:$port")
                        clipboard.setPrimaryClip(clip)
                        showSnackbar(getString(R.string.copied_to_clipboard))
                    }
                    2 -> {
                        removeCustomServer(host, port)
                    }
                }
            }
            .show()
    }
    
    private fun addCustomServerChip(name: String, host: String, port: Int) {
        if (customServers.any { it.second.first == host && it.second.second == port }) {
            showSnackbar("Server already exists")
            return
        }
        
        val chip = layoutInflater.inflate(R.layout.item_chip, chipGroup, false) as Chip
        chip.text = name
        chip.isCheckedIconVisible = false
        chip.isCloseIconVisible = true
        
        chip.setOnClickListener {
            etHostname.setText(host)
            etPort.setText(port.toString())
            connectToDashboard()
        }
        
        chip.setOnCloseIconClickListener {
            removeCustomServer(host, port)
            chipGroup.removeView(chip)
        }
        
        chip.setOnLongClickListener {
            showServerOptionsDialog(host, port, name)
            true
        }
        
        chipGroup.addView(chip, chipGroup.indexOfChild(chipAdd))
        customServers.add(name to (host to port))
        saveCustomServers()
    }
    
    private fun removeCustomServer(host: String, port: Int) {
        customServers.removeAll { it.second.first == host && it.second.second == port }
        saveCustomServers()
        
        for (i in 0 until chipGroup.childCount) {
            val child = chipGroup.getChildAt(i)
            if (child is Chip && child != chipLocalhost && child != chipAdd) {
                chipGroup.removeView(child)
                break
            }
        }
    }
    
    private fun saveCustomServers() {
        val prefs = getSharedPreferences("custom_servers", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("server_count", customServers.size)
        customServers.forEachIndexed { index, (name, pair) ->
            editor.putString("server_${index}_name", name)
            editor.putString("server_${index}_host", pair.first)
            editor.putInt("server_${index}_port", pair.second)
        }
        editor.apply()
    }
    
    private fun loadCustomServers() {
        val prefs = getSharedPreferences("custom_servers", Context.MODE_PRIVATE)
        val count = prefs.getInt("server_count", 0)
        
        for (i in 0 until count) {
            val name = prefs.getString("server_${i}_name", "") ?: ""
            val host = prefs.getString("server_${i}_host", "") ?: ""
            val port = prefs.getInt("server_${i}_port", 9090)
            
            if (name.isNotEmpty() && host.isNotEmpty()) {
                addCustomServerChip(name, host, port)
            }
        }
    }
    
    private fun updateConnectionStatus(connected: Boolean, detail: String?) {
        runOnUiThread {
            if (connected) {
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_connected)
                statusText.text = getString(R.string.status_connected)
                statusDetail.text = detail ?: currentUrl ?: getString(R.string.status_connected)
            } else {
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_disconnected)
                statusText.text = getString(R.string.status_disconnected)
                statusDetail.text = "-"
            }
        }
    }
    
    private fun updateButtonStates(connected: Boolean) {
        btnConnect.isEnabled = !connected
        btnDisconnect.isEnabled = connected
        btnOpenBrowser.isEnabled = connected
    }
    
    private fun clearWebViewCache() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        showSnackbar(getString(R.string.cache_cleared))
    }
    
    private fun checkNetworkStatus() {
        mainScope.launch {
            while (true) {
                val isOnline = ClientUtils.isNetworkAvailable(this@MainActivity)
                if (!isOnline && isConnected) {
                    showSnackbar(getString(R.string.network_lost))
                    forceDisconnectFromDashboard()
                }
                delay(5000)
            }
        }
    }
    
    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    private fun showInfoDialog() {
        val info = getString(R.string.info_message, BuildConfig.VERSION_NAME)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage(info)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton("GitHub") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))
                    startActivity(intent)
                } catch (e: Exception) {
                    showSnackbar("Cannot open browser")
                }
            }
            .show()
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        webView.destroy()
    }
}
