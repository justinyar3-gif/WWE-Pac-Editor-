package com.modplacewii.paceditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.util.Base64
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingOpenUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pendingOpenUri?.let { openUri(it) }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("*/*"))
                }
                startActivityForResult(intent, REQUEST_PICK_FILE)
                return true
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
        pendingOpenUri = intent?.data
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.data != null) openUri(intent.data)
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE) {
            val uri = if (resultCode == RESULT_OK) data?.data else null
            fileChooserCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
            fileChooserCallback = null
        }
    }

    private fun openUri(uri: Uri?) {
        if (uri == null) return
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Android could not read this file")
            val name = queryDisplayName(uri) ?: (uri.lastPathSegment ?: "opened.pac")
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val safeName = name.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript("window.loadNativeFile('$safeName','$encoded');", null)
            pendingOpenUri = null
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open PAC file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun saveFile(name: String, base64: String, mimeType: String?) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val safeName = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "export.bin" }
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
                    if (Build.VERSION.SDK_INT >=  Q) put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = if (Build.VERSION.SDK_INT >= Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI
                                 else MediaStore.Files.getContentUri("external")
                val uri = contentResolver.insert(collection, values) ?: throw IOException("Could not create Downloads file")
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IOException("Could not write file")
                if (Build.VERSION.SDK_INT >= Q) {
                    val done = android.content.ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    contentResolver.update(uri, done, null, null)
                }
                runOnUiThread { Toast.makeText(this@MainActivity, "Saved to Downloads/$safeName", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val REQUEST_PICK_FILE = 4102
        private const val Q = 29
    }
}
