package com.udyneos.zashapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {
    private WebView mWebView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NetworkCallback networkCallback;
    
    // Variables for file upload
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private String[] mCaptureFileTypes = {"image/*", "video/*"};
    private boolean mCameraPhotoSelected = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mWebView = findViewById(R.id.activity_main_webview);

        // Konfigurasi SwipeRefreshLayout
        configureSwipeRefresh();

        // Konfigurasi WebView
        configureWebView();

        // Load URL awal berdasarkan koneksi jaringan
        loadInitialUrl();

        // Setup network monitoring
        setupNetworkMonitoring();
    }

    private void configureSwipeRefresh() {
        // Atur warna animasi loading
        mSwipeRefreshLayout.setColorSchemeColors(
            getColor(R.color.refresh_color1),    // Merah
            getColor(R.color.refresh_color2),    // Hijau
            getColor(R.color.refresh_color3)     // Biru
        );

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.WHITE);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            if (mWebView != null) {
                mWebView.reload();
            }
        });
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongConstant"})
    private void configureWebView() {
        WebSettings webSettings = mWebView.getSettings();
        
        // Enable JavaScript dan fitur WebView
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webSettings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        
        mWebView.setBackgroundColor(Color.WHITE);
        
        // WebChromeClient untuk upload file
        mWebView.setWebChromeClient(new MyWebChromeClient());
        
        // WebViewClient untuk menangani refresh animation
        mWebView.setWebViewClient(new HelloWebViewClient());
        
        // Download listener
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, 
                URLUtil.guessFileName(url, contentDisposition, mimetype));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
        });
    }

    private void loadInitialUrl() {
        if (isNetworkAvailable()) {
            mWebView.loadUrl("http://127.0.0.1:9090/ui");
        } else {
            mWebView.loadUrl("file:///android_asset/offline.html");
        }
    }

    // ==================== UPLOAD FILE HANDLING ====================
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode != FILE_CHOOSER_RESULT_CODE || mUploadMessage == null) {
            return;
        }
        
        Uri[] results = null;
        
        // Check if response is positive
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoSelected && mUploadMessage != null) {
                    // Handle camera photo case
                    results = new Uri[]{Uri.parse("file://" + mCameraPhotoSelected)};
                }
            } else {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();
                
                if (clipData != null) {
                    // Multiple files selected
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                } else if (dataString != null) {
                    // Single file selected
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        
        // Return the selected files to WebView
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(results);
            mUploadMessage = null;
        }
        
        mCameraPhotoSelected = false;
    }
    
    // Custom WebChromeClient untuk handle file upload
    private class MyWebChromeClient extends WebChromeClient {
        // For Android 5.0+
        @Override
        public boolean onShowFileChooser(WebView webView, 
                ValueCallback<Uri[]> filePathCallback, 
                FileChooserParams fileChooserParams) {
            
            // Make sure there is no existing message
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
            }
            mUploadMessage = filePathCallback;
            
            Intent takePictureIntent = null;
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            
            // Set file types
            if (fileChooserParams != null && fileChooserParams.getAcceptTypes() != null) {
                String[] acceptTypes = fileChooserParams.getAcceptTypes();
                if (acceptTypes.length > 0 && !acceptTypes[0].equals("*/*")) {
                    contentSelectionIntent.setType(String.join(",", acceptTypes));
                    contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
                } else {
                    contentSelectionIntent.setType("*/*");
                }
            } else {
                contentSelectionIntent.setType("*/*");
            }
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            
            // Create chooser intent
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File");
            
            // Start activity
            try {
                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            } catch (Exception e) {
                mUploadMessage = null;
                Toast.makeText(MainActivity.this, "Cannot open file chooser", Toast.LENGTH_LONG).show();
                return false;
            }
            
            return true;
        }
        
        // Handle permission requests (for camera, microphone, etc.)
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Grant all permissions for simplicity
                request.grant(request.getResources());
            }
        }
        
        // For showing JavaScript alerts
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            result.confirm();
            return true;
        }
    }
    
    // ==================== WEBVIEW CLIENT ====================
    
    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            
            // Inject JavaScript untuk file upload support
            injectFileUploadSupport(view);
            injectLightModeCSS(view);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
        
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    
    // Inject JavaScript untuk file upload support
    private void injectFileUploadSupport(WebView webView) {
        String js = """
            // Enhance file input click handling
            document.addEventListener('click', function(e) {
                if (e.target.type === 'file') {
                    e.target.addEventListener('change', function() {
                        console.log('File selected: ', this.files.length + ' files');
                    });
                }
            });
            
            // Make sure all file inputs are visible
            var style = document.createElement('style');
            style.textContent = 'input[type="file"] { opacity: 1 !important; visibility: visible !important; }';
            document.head.appendChild(style);
            
            console.log('File upload support injected');
            """;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        }
    }

    private void injectLightModeCSS(WebView webView) {
        String css = ":root { color-scheme: light only; } body { background-color: white !important; color: black !important; }";
        String js = "var style = document.createElement('style'); style.textContent = `" + css + "`; document.head.appendChild(style);";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
               actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
               actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || 
               actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }

    private void setupNetworkMonitoring() {
        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (mWebView != null && !mWebView.getUrl().startsWith("file:///android_asset")) {
                        mWebView.loadUrl("http://127.0.0.1:9090/ui");
                    }
                });
            }
            
            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (mWebView != null && mWebView.getUrl() != null) {
                        mWebView.loadUrl("file:///android_asset/offline.html");
                    }
                });
            }
        };
        
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}