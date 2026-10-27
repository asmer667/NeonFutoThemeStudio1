package com.futo.nativestudio

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutoTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun FutoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8B5CF6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) { content() }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun sendJs(js: String) {
        webViewRef?.evaluateJavascript(js, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FUTO THEME STUDIO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { 
                        isArabic = !isArabic
                        val lang = if (isArabic) "ar" else "en"
                        sendJs("window.FutoNativeBridge && window.FutoNativeBridge.setLanguage('$lang');")
                    }) {
                        Text(if (isArabic) "عربي" else "EN", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF18181B))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF18181B)) {
                val navItems = listOf(
                    Triple("الرئيسية", Icons.Default.Home, "window.location.reload();"),
                    Triple("الألوان", Icons.Default.Palette, "document.querySelectorAll('.tabs button')[2]?.click();"),
                    Triple("المعاينة", Icons.Outlined.Visibility, "document.querySelector('.workcanvas')?.scrollIntoView();"),
                    Triple("استيراد", Icons.Default.Download, "window.FutoNativeBridge && window.FutoNativeBridge.openImport();"),
                    Triple("تصدير", Icons.Default.Share, "window.FutoNativeBridge && window.FutoNativeBridge.openExport();")
                )
                
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { sendJs(item.third) },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        
                        addJavascriptInterface(WebAppInterface(ctx), "AndroidBridge")
                        loadUrl("file:///android_asset/www/index.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

class WebAppInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun saveZip(base64Data: String, fileName: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val os: OutputStream = context.contentResolver.openOutputStream(uri)!!
                    os.write(bytes)
                    os.close()
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(bytes) }
            }
            Toast.makeText(context, "تم الحفظ في Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "فشل الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
