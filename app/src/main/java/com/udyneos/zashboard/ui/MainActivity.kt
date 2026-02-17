package com.udyneos.zashboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.udyneos.zashboard.BuildConfig
import com.udyneos.zashboard.R
import com.udyneos.zashboard.adapters.ViewPagerAdapter
import com.udyneos.zashboard.models.ServerConfig
import com.udyneos.zashboard.utils.ClientUtils
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: ViewPagerAdapter
    
    // Control views - nullable
    private var statusIndicator: View? = null
    private var statusText: TextView? = null
    private var statusDetail: TextView? = null
    private var progressBar: View? = null
    private var switchConnection: SwitchCompat? = null
    private var chipGroup: ChipGroup? = null
    private var chipLocalhost: Chip? = null
    private var chipAdd: Chip? = null
    private var etHostname: TextInputEditText? = null
    private var etPort: TextInputEditText? = null
    private var etSecret: TextInputEditText? = null
    private var btnOpenBrowser: MaterialButton? = null
    
    // WebView
    var webView: WebView? = null
        private set
    
    private var currentConfig = ServerConfig()
    
    // Status koneksi
    private var isConnected = false
        set(value) {
            if (field != value) {  // Hanya update jika berubah
                field = value
                runOnUiThread {
                    updateUIForConnectionState(value)
                }
            }
        }
    
    private var isDisconnecting = false  // Flag untuk mencegah double disconnect
    
    var currentUrl: String? = null
        set(value) {
            field = value
            runOnUiThread {
                statusDetail?.text = value ?: "-"
            }
        }
    
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val customServers = mutableListOf<Pair<String, Pair<String, Int>>>()
    
    // Flag untuk mencegah rekursi switch
    private var isSwitchChanging = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Hide toolbar
        supportActionBar?.hide()
        
        initViews()
        setupViewPager()
        loadCustomServers()
        setupOnBackPressedDispatcher()
        checkNetworkStatus()
        
        // Initial state
        isConnected = false
        currentUrl = null
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
    }
    
    private fun setupViewPager() {
        pagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Control"
                1 -> tab.text = "Dashboard"
            }
        }.attach()
        
        viewPager.offscreenPageLimit = 2
    }
    
    fun updateWebView(webView: WebView) {
        this.webView = webView
        setupWebView(webView)
    }
    
    private fun setupWebView(webView: WebView) {
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
        
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.isNestedScrollingEnabled = true
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                runOnUiThread {
                    progressBar?.visibility = View.VISIBLE
                }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                runOnUiThread {
                    progressBar?.visibility = View.GONE
                    
                    // CEK: Jika sedang dalam proses disconnect, abaikan
                    if (isDisconnecting) {
                        return@runOnUiThread
                    }
                    
                    // CEK: Jika URL adalah about:blank, jangan set connected
                    if (url == "about:blank") {
                        // Pastikan status disconnected
                        if (isConnected) {
                            isConnected = false
                        }
                    } else {
                        // Update ke connected state
                        currentUrl = url
                        isConnected = true
                        
                        // Update switch ON (tanpa trigger listener)
                        isSwitchChanging = true
                        switchConnection?.isChecked = true
                        isSwitchChanging = false
                    }
                }
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    runOnUiThread {
                        progressBar?.visibility = View.GONE
                        val errorMsg = error?.description?.toString() ?: "Unknown error"
                        showSnackbar("Failed to load: $errorMsg")
                        
                        // Pastikan disconnected
                        isConnected = false
                        currentUrl = null
                        
                        // Update switch OFF (tanpa trigger listener)
                        isSwitchChanging = true
                        switchConnection?.isChecked = false
                        isSwitchChanging = false
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
                        progressBar?.visibility = View.GONE
                        val statusCode = errorResponse?.statusCode ?: 0
                        showSnackbar("Server error: $statusCode")
                        
                        // Pastikan disconnected
                        isConnected = false
                        currentUrl = null
                        
                        // Update switch OFF (tanpa trigger listener)
                        isSwitchChanging = true
                        switchConnection?.isChecked = false
                        isSwitchChanging = false
                    }
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        }
    }
    
    fun registerControlViews(
        statusIndicator: View,
        statusText: TextView,
        statusDetail: TextView,
        progressBar: View,
        switchConnection: SwitchCompat,
        chipGroup: ChipGroup,
        chipLocalhost: Chip,
        chipAdd: Chip,
        etHostname: TextInputEditText,
        etPort: TextInputEditText,
        etSecret: TextInputEditText,
        btnOpenBrowser: MaterialButton
    ) {
        this.statusIndicator = statusIndicator
        this.statusText = statusText
        this.statusDetail = statusDetail
        this.progressBar = progressBar
        this.switchConnection = switchConnection
        this.chipGroup = chipGroup
        this.chipLocalhost = chipLocalhost
        this.chipAdd = chipAdd
        this.etHostname = etHostname
        this.etPort = etPort
        this.etSecret = etSecret
        this.btnOpenBrowser = btnOpenBrowser
        
        setupControlViews()
    }
    
    private fun setupControlViews() {
        // SWITCH CONNECTION
        switchConnection?.setOnCheckedChangeListener { _, isChecked ->
            // Cegah rekursi
            if (isSwitchChanging) return@setOnCheckedChangeListener
            
            if (isChecked) {
                // Switch ON -> Connect
                isDisconnecting = false
                connectToDashboard()
            } else {
                // Switch OFF -> Disconnect
                disconnectFromDashboard()
            }
        }
        
        // Open in browser
        btnOpenBrowser?.setOnClickListener {
            openInBrowser()
        }
        
        // Localhost chip
        chipLocalhost?.setOnClickListener {
            etHostname?.setText("127.0.0.1")
            etPort?.setText("9090")
            etSecret?.setText("")
            showSnackbar(getString(R.string.set_to_localhost))
        }
        
        chipLocalhost?.setOnLongClickListener {
            showServerOptionsDialog("127.0.0.1", 9090, "localhost")
            true
        }
        
        // Add custom server chip
        chipAdd?.setOnClickListener {
            showAddServerDialog()
        }
        
        // Initial state
        isConnected = false
        currentUrl = null
        updateUIForConnectionState(false)
    }
    
    private fun updateUIForConnectionState(connected: Boolean) {
        if (connected) {
            // Connected
            statusIndicator?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_connected)
            statusText?.text = getString(R.string.status_connected)
            btnOpenBrowser?.isEnabled = true
            
            // Update switch ON jika perlu
            if (switchConnection?.isChecked == false) {
                isSwitchChanging = true
                switchConnection?.isChecked = true
                isSwitchChanging = false
            }
        } else {
            // Disconnected
            statusIndicator?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_disconnected)
            statusText?.text = getString(R.string.status_disconnected)
            statusDetail?.text = "-"
            btnOpenBrowser?.isEnabled = false
            
            // Update switch OFF jika perlu
            if (switchConnection?.isChecked == true) {
                isSwitchChanging = true
                switchConnection?.isChecked = false
                isSwitchChanging = false
            }
        }
    }
    
    // ============================================
    // METHOD CONNECT
    // ============================================
    private fun connectToDashboard() {
        val hostname = etHostname?.text?.toString()?.trim()
        val portText = etPort?.text?.toString()?.trim()
        
        if (hostname.isNullOrEmpty()) {
            etHostname?.error = getString(R.string.hostname_required)
            // Switch kembali ke OFF
            isSwitchChanging = true
            switchConnection?.isChecked = false
            isSwitchChanging = false
            return
        }
        
        val port = portText?.toIntOrNull() ?: 9090
        
        if (!ClientUtils.isNetworkAvailable(this)) {
            showSnackbar(getString(R.string.no_network))
            // Switch kembali ke OFF
            isSwitchChanging = true
            switchConnection?.isChecked = false
            isSwitchChanging = false
            return
        }
        
        currentConfig = ServerConfig(
            hostname = hostname,
            port = port,
            secret = etSecret?.text?.toString()?.trim() ?: "",
            disableUpgradeCore = false
        )
        
        val url = "http://$hostname:$port/ui/"
        
        // Tampilkan progress
        progressBar?.visibility = View.VISIBLE
        statusText?.text = getString(R.string.status_connecting)
        
        // Load di WebView
        webView?.loadUrl(url)
        
        // Swipe ke halaman Dashboard
        viewPager.currentItem = 1
    }
    
    // ============================================
    // METHOD DISCONNECT - FIX UNTUK SWITCH OFF PERTAMA
    // ============================================
    private fun disconnectFromDashboard() {
        // Set flag disconnecting
        isDisconnecting = true
        
        // STEP 1: Update UI ke disconnected LANGSUNG
        runOnUiThread {
            statusIndicator?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_disconnected)
            statusText?.text = getString(R.string.status_disconnected)
            statusDetail?.text = "-"
            btnOpenBrowser?.isEnabled = false
            progressBar?.visibility = View.GONE
            
            // PASTIKAN switch OFF
            if (switchConnection?.isChecked == true) {
                isSwitchChanging = true
                switchConnection?.isChecked = false
                isSwitchChanging = false
            }
        }
        
        // STEP 2: Reset state
        isConnected = false
        currentUrl = null
        
        // STEP 3: Hentikan semua aktivitas WebView
        webView?.stopLoading()
        webView?.loadUrl("about:blank")
        webView?.clearHistory()
        webView?.clearCache(true)
        webView?.clearFormData()
        
        // STEP 4: Tampilkan pesan
        showSnackbar("Disconnected")
        
        // STEP 5: PASTIKAN WebView benar-benar blank
        Handler(Looper.getMainLooper()).postDelayed({
            webView?.loadUrl("about:blank")
            
            // Reset flag setelah semua selesai
            isDisconnecting = false
            
            // STEP 6: Final check status
            runOnUiThread {
                // Pastikan status masih disconnected
                if (isConnected) {
                    isConnected = false
                }
            }
        }, 200)
    }
    
    // ============================================
    // METHOD OPEN BROWSER
    // ============================================
    private fun openInBrowser() {
        if (!isConnected) {
            showSnackbar(getString(R.string.not_connected))
            return
        }
        
        val url = webView?.url
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
                        etHostname?.setText(host)
                        etPort?.setText(port.toString())
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
            etHostname?.setText(host)
            etPort?.setText(port.toString())
            connectToDashboard()
        }
        
        chip.setOnCloseIconClickListener {
           removeCustomServer(host, port)
            chipGroup?.removeView(chip)
        }
        
        chip.setOnLongClickListener {
            showServerOptionsDialog(host, port, name)
            true
        }
        
        chipGroup?.addView(chip, chipGroup?.indexOfChild(chipAdd) ?: 0)
        customServers.add(name to (host to port))
        saveCustomServers()
    }
    
    private fun removeCustomServer(host: String, port: Int) {
        customServers.removeAll { it.second.first == host && it.second.second == port }
        saveCustomServers()
        
        chipGroup?.let { group ->
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is Chip && child != chipLocalhost && child != chipAdd) {
                    group.removeView(child)
                    break
                }
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
    
    fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun checkNetworkStatus() {
        mainScope.launch {
            while (true) {
                val isOnline = ClientUtils.isNetworkAvailable(this@MainActivity)
                if (!isOnline && isConnected) {
                    showSnackbar(getString(R.string.network_lost))
                    disconnectFromDashboard()
                }
                delay(5000)
            }
        }
    }
    
    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem == 1) {
                    viewPager.currentItem = 0
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        webView?.destroy()
    }
}
