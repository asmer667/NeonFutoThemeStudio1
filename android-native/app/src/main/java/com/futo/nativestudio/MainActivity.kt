package com.futo.nativestudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AssetItem(
    val name: String, val selector: String, val type: String, var bitmap: Bitmap? = null,
    var sliceTop: Float = 0.3f, var sliceBottom: Float = 0.7f, var sliceLeft: Float = 0.3f, var sliceRight: Float = 0.7f
)

fun createGradientBackground(c1: Int, c2: Int): Bitmap {
    val width = 400; val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), c1, c2, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bitmap
}

fun createIconBitmap(type: String, color: Int): Bitmap {
    val size = 200; val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.strokeWidth = 15f; this.style = Paint.Style.STROKE; this.strokeCap = Paint.Cap.ROUND }
    when (type) {
        "backspace" -> { val path = android.graphics.Path().apply { moveTo(50f, 150f); lineTo(150f, 150f); lineTo(180f, 100f); lineTo(150f, 50f); lineTo(50f, 50f); close() }; canvas.drawPath(path, paint); canvas.drawLine(80f, 75f, 130f, 125f, paint); canvas.drawLine(130f, 75f, 80f, 125f, paint) }
        "enter" -> { val path = android.graphics.Path().apply { moveTo(150f, 50f); lineTo(150f, 150f); lineTo(50f, 150f) }; canvas.drawPath(path, paint); val arrow = android.graphics.Path().apply { moveTo(50f, 150f); lineTo(80f, 120f); moveTo(50f, 150f); lineTo(80f, 180f) }; canvas.drawPath(arrow, paint) }
        "shift" -> { val path = android.graphics.Path().apply { moveTo(100f, 40f); lineTo(160f, 100f); lineTo(130f, 100f); lineTo(130f, 160f); lineTo(70f, 160f); lineTo(70f, 100f); lineTo(40f, 100f); close() }; canvas.drawPath(path, paint) }
        "space" -> { canvas.drawLine(50f, 120f, 150f, 120f, paint); canvas.drawLine(50f, 120f, 70f, 100f, paint); canvas.drawLine(50f, 120f, 70f, 140f, paint) }
    }
    return bitmap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FutoTheme { AppRoot() } }
    }
}

@Composable
fun FutoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8B5CF6), background = Color(0xFF121212), surface = Color(0xFF1E1E1E), onBackground = Color.White, onSurface = Color.White)) { content() }
}

@Composable
fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1000); showSplash = false }
    if (showSplash) SplashScreen() else MainScreen()
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF8B5CF6)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("FUTO Theme Studio", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("futo_theme_prefs", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var themeName by remember { mutableStateOf(prefs.getString("theme_name", "My Custom Theme") ?: "My Custom Theme") }
    var themeAuthor by remember { mutableStateOf(prefs.getString("theme_author", "Unknown Author") ?: "Unknown Author") }
    var keyboardBg by remember { mutableStateOf(Color(prefs.getInt("keyboard_bg", Color(0xFF333333).toArgb()))) }
    var keyBg by remember { mutableStateOf(Color(prefs.getInt("key_bg", Color(0xFF666666).toArgb()))) }
    var textColor by remember { mutableStateOf(Color(prefs.getInt("text_color", Color.White.toArgb()))) }
    
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fontName by remember { mutableStateOf<String?>(null) }
    var fontBytes by remember { mutableStateOf<ByteArray?>(null) }
    var assets by remember { mutableStateOf(listOf(AssetItem("btn_normal.png", "normal", "border"), AssetItem("btn_pressed.png", "pressed", "border"), AssetItem("btn_enter.png", "action", "border"))) }
    var currentAssetIndex by remember { mutableStateOf(-1) }
    var showAbout by remember { mutableStateOf(false) }

    fun saveState() {
        prefs.edit().apply {
            putBoolean("dark_mode", isDarkMode); putString("theme_name", themeName); putString("theme_author", themeAuthor)
            putInt("keyboard_bg", keyboardBg.toArgb()); putInt("key_bg", keyBg.toArgb()); putInt("text_color", textColor.toArgb())
            apply()
        }
    }
    LaunchedEffect(themeName, themeAuthor, keyboardBg, keyBg, textColor, isDarkMode) { saveState() }

    val bgPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) backgroundBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri)) }
    val fontPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { fontName = "custom_font.ttf"; fontBytes = context.contentResolver.openInputStream(uri)?.readBytes() } }
    val assetPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && currentAssetIndex >= 0) {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            val updatedList = assets.toMutableList(); updatedList[currentAssetIndex] = updatedList[currentAssetIndex].copy(bitmap = bitmap); assets = updatedList; currentAssetIndex = -1
        }
    }
    val projectPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString != null) {
                    val json = JSONObject(jsonString)
                    themeName = json.optString("name", "Imported Theme"); themeAuthor = json.optString("author", "Unknown")
                    keyboardBg = Color(json.optInt("bgColor", Color(0xFF333333).toArgb())); keyBg = Color(json.optInt("keyColor", Color(0xFF666666).toArgb())); textColor = Color(json.optInt("textColor", Color.White.toArgb()))
                    Toast.makeText(context, "تم استيراد المشروع", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Toast.makeText(context, "فشل الاستيراد", Toast.LENGTH_SHORT).show() }
        }
    }

    fun saveProject() {
        try {
            val json = JSONObject().apply { put("name", themeName); put("author", themeAuthor); put("bgColor", keyboardBg.toArgb()); put("keyColor", keyBg.toArgb()); put("textColor", textColor.toArgb()) }
            val fileName = "${themeName.replace(" ", "_")}_project.json"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "application/json"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) { val os = context.contentResolver.openOutputStream(uri)!!; os.write(json.toString().toByteArray()); os.close() }
            }
            Toast.makeText(context, "تم حفظ المشروع", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(context, "فشل الحفظ", Toast.LENGTH_SHORT).show() }
    }

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColorUI = if (isDarkMode) Color.White else Color.Black

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme(primary = Color(0xFF8B5CF6), background = bgColor, surface = surfaceColor, onBackground = textColorUI, onSurface = textColorUI)
        else lightColorScheme(primary = Color(0xFF8B5CF6), background = bgColor, surface = surfaceColor, onBackground = textColorUI, onSurface = textColorUI)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FUTO Theme Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); isDarkMode = !isDarkMode }) { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Mode") }
                        IconButton(onClick = { saveProject() }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                        IconButton(onClick = { projectPickerLauncher.launch("*/*") }) { Icon(Icons.Default.FolderOpen, contentDescription = "Load") }
                        IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, contentDescription = "About") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor, titleContentColor = textColorUI, actionIconContentColor = textColorUI)
                )
                if (showAbout) {
                    AlertDialog(
                        onDismissRequest = { showAbout = false },
                        title = { Text("حول التطبيق") },
                        text = { Text("FUTO Theme Studio v1.0\n\nتطبيق أصلي لإنشاء وتعديل ثيمات لوحة مفاتيح FUTO Keyboard.\n\nتم بناؤه باستخدام Kotlin و Jetpack Compose.") },
                        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("حسناً") } }
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = surfaceColor) {
                    val navItems = listOf(
                        Triple("المحرر", Icons.Default.Palette, 0),
                        Triple("الخلفيات", Icons.Default.Image, 1),
                        Triple("الأيقونات", Icons.Default.Star, 2),
                        Triple("تصدير", Icons.Default.Share, 3)
                    )
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = selectedTab == item.third, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); selectedTab = item.third },
                            icon = { Icon(item.second, contentDescription = item.first) }, label = { Text(item.first, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF8B5CF6), selectedTextColor = Color(0xFF8B5CF6), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                modifier = Modifier.padding(padding).fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> EditorScreen(
                        themeName, { themeName = it }, themeAuthor, { themeAuthor = it },
                        keyboardBg, { keyboardBg = it }, keyBg, { keyBg = it }, textColor, { textColor = it },
                        onPickBg = { bgPickerLauncher.launch("image/*") }, onPickFont = { fontPickerLauncher.launch("*/*") },
                        fontName = fontName, hasBackground = backgroundBitmap != null,
                        previewContent = { InteractiveKeyboardPreview(keyboardBg, keyBg, textColor, backgroundBitmap, scope, haptic) }
                    )
                    1 -> BackgroundGalleryScreen(
                        onSelect = { bmp -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); backgroundBitmap = bmp; Toast.makeText(context, "تم تطبيق الخلفية", Toast.LENGTH_SHORT).show() },
                        onUpload = { bgPickerLauncher.launch("image/*") }
                    )
                    2 -> IconsScreen(onSelectIcon = { iconType, selector -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); val bmp = createIconBitmap(iconType, textColor.toArgb()); val list = assets.toMutableList(); list.add(AssetItem("icon_$iconType.png", selector, "icon", bmp)); assets = list; Toast.makeText(context, "تمت الإضافة", Toast.LENGTH_SHORT).show() })
                    3 -> ExportScreen { exportCompleteTheme(context, themeName, themeAuthor, keyboardBg, keyBg, textColor, backgroundBitmap, fontName, fontBytes, assets) }
                }
            }
        }
    }
}

@Composable
fun BackgroundGalleryScreen(onSelect: (Bitmap) -> Unit, onUpload: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("مكتبة الخلفيات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text("اختر خلفية جاهزة أو ارفع صورة من هاتفك", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onUpload, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("رفع صورة من الهاتف", color = MaterialTheme.colorScheme.onSurface) }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        val presets = listOf(
            "بنفسجي" to createGradientBackground(0xFF8B5CF6.toInt(), 0xFF2E1065.toInt()),
            "غروب" to createGradientBackground(0xFFFF7E5F.toInt(), 0xFFFEB47B.toInt()),
            "سماء" to createGradientBackground(0xFF00C6FF.toInt(), 0xFF0072FF.toInt()),
            "ليل" to createGradientBackground(0xFF0F2027.toInt(), 0xFF203A43.toInt()),
            "نعناع" to createGradientBackground(0xFF00B4DB.toInt(), 0xFF0083B0.toInt()),
            "وردي" to createGradientBackground(0xFFFF9A9E.toInt(), 0xFFFAD0C4.toInt())
        )

        presets.forEach { (name, bmp) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(bmp) }
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = name, modifier = Modifier.size(60.dp, 40.dp).background(Color.DarkGray, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                Text(name, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun InteractiveKeyboardPreview(keyboardBg: Color, keyBg: Color, textColor: Color, backgroundBitmap: Bitmap?, scope: CoroutineScope, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    var pressedKey by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(keyboardBg, RoundedCornerShape(8.dp))) {
        if (backgroundBitmap != null) {
            Image(bitmap = backgroundBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val rows = listOf(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"))
            rows.forEach { row ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { letter -> 
                        val isPressed = pressedKey == letter
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (isPressed) keyBg.copy(alpha = 0.6f) else keyBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pressedKey = letter
                                    scope.launch { delay(150); if (pressedKey == letter) pressedKey = null }
                                }, 
                            contentAlignment = Alignment.Center
                        ) { 
                            Text(letter, color = textColor, fontSize = if (isPressed) 16.sp else 14.sp) 
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun IconsScreen(onSelectIcon: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("مكتبة الأيقونات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        listOf(Triple("backspace", "أيقونة المسح (Backspace)", "icon delete_key"), Triple("enter", "أيقونة الإدخال (Enter)", "icon enter_key"), Triple("shift", "أيقونة التحويل (Shift)", "icon shift_key"), Triple("space", "أيقونة المسافة (Space)", "icon space_key")).forEach { (type, label, selector) ->
            Button(onClick = { onSelectIcon(type, selector) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(label, color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

fun exportCompleteTheme(context: Context, name: String, author: String, bgColor: Color, keyColor: Color, textColor: Color, backgroundImage: Bitmap?, fontName: String?, fontBytes: ByteArray?, assets: List<AssetItem>) {
    try {
        var toml = "name = \"$name\"\nauthor = \"$author\"\nversion = 1\n[colors]\nkeyboard_surface = \"#${bgColor.toArgb().toUInt().toString(16).takeLast(6)}\"\nkeyboard_container = \"#${keyColor.toArgb().toUInt().toString(16).takeLast(6)}\"\non_keyboard_container = \"#${textColor.toArgb().toUInt().toString(16).takeLast(6)}\""
        if (fontName != null) toml += "\n\n[options.font]\nfont = \"$fontName\""
        if (backgroundImage != null) toml += "\n\n[options.background]\nimage = \"background.png\"\nopacity = 1.0"
        assets.filter { it.bitmap != null }.forEach { toml += "\n\n[[matchrules.${it.type}]]\nselector = \"${it.selector}\"\nasset = \"${it.name}\"" }
        assets.filter { it.bitmap != null }.forEach { if (it.type == "border") toml += "\n\n[[asset.border]]\nname = \"${it.name}\"\nbackground_tint = \"#ffffff\"\nforeground_tint = \"#ffffff\"\npadding = [0, 0, 0, 0]\nslicing = [${it.sliceTop}, ${it.sliceRight}, ${it.sliceBottom}, ${it.sliceLeft}]\n" else toml += "\n\n[[asset.icon]]\nname = \"${it.name}\"\ntarget_density = 640\n" }
        val baos = ByteArrayOutputStream(); val zos = ZipOutputStream(baos)
        zos.putNextEntry(ZipEntry("theme.txt")); zos.write(toml.toByteArray()); zos.closeEntry()
        if (backgroundImage != null) { val b = ByteArrayOutputStream(); backgroundImage.compress(Bitmap.CompressFormat.PNG, 100, b); zos.putNextEntry(ZipEntry("background.png")); zos.write(b.toByteArray()); zos.closeEntry() }
        if (fontName != null && fontBytes != null) { zos.putNextEntry(ZipEntry(fontName)); zos.write(fontBytes); zos.closeEntry() }
        assets.filter { it.bitmap != null }.forEach { val b = ByteArrayOutputStream(); it.bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, b); zos.putNextEntry(ZipEntry(it.name)); zos.write(b.toByteArray()); zos.closeEntry() }
        zos.close()
        val fileName = "${name.replace(" ", "_")}.zip"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "application/zip"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }; val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values); if (uri != null) { val os = context.contentResolver.openOutputStream(uri)!!; os.write(baos.toByteArray()); os.close() } }
        Toast.makeText(context, "تم الحفظ: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) { Toast.makeText(context, "فشل: ${e.message}", Toast.LENGTH_LONG).show() }
}

@Composable
fun EditorScreen(themeName: String, onNameChange: (String) -> Unit, themeAuthor: String, onAuthorChange: (String) -> Unit, keyboardBg: Color, onBgChange: (Color) -> Unit, keyBg: Color, onKeyChange: (Color) -> Unit, textColor: Color, onTextChange: (Color) -> Unit, onPickBg: () -> Unit, onPickFont: () -> Unit, fontName: String?, hasBackground: Boolean, previewContent: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        previewContent(); Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = themeName, onValueChange = onNameChange, label = { Text("اسم الثيم") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)); Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = themeAuthor, onValueChange = onAuthorChange, label = { Text("المصمم") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)); Spacer(modifier = Modifier.height(16.dp))
        ColorPickerRow("لون الخلفية", keyboardBg, onBgChange); Spacer(modifier = Modifier.height(8.dp)); ColorPickerRow("لون المفاتيح", keyBg, onKeyChange); Spacer(modifier = Modifier.height(8.dp)); ColorPickerRow("لون النص", textColor, onTextChange); Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onPickBg, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(if (hasBackground) "تغيير صورة الخلفية" else "اختيار صورة خلفية", color = MaterialTheme.colorScheme.onSurface) }; Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onPickFont, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(if (fontName != null) "تغيير الخط ($fontName)" else "اختيار خط", color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
fun AssetsScreen(assets: List<AssetItem>, onPickImage: (Int) -> Unit, onUpdateAsset: (Int, AssetItem) -> Unit) {
    var expandedIndex by remember { mutableStateOf(-1) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("إدارة الأصول (Assets)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.height(16.dp))
        assets.forEachIndexed { index, asset ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                Row(modifier = Modifier.fillMaxWidth().clickable { expandedIndex = if (expandedIndex == index) -1 else index }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    asset.bitmap?.let { bmp -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp)) } ?: Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp)); Text(asset.name, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.weight(1f)); Icon(if (expandedIndex == index) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                }
                if (expandedIndex == index) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(onClick = { onPickImage(index) }, modifier = Modifier.fillMaxWidth()) { Text("تغيير الصورة") }
                        SliderSetting("قص أعلى", asset.sliceTop, 0f, 1f, { asset.sliceTop = it; onUpdateAsset(index, asset.copy()) }); SliderSetting("قص أسفل", asset.sliceBottom, 0f, 1f, { asset.sliceBottom = it; onUpdateAsset(index, asset.copy()) }); SliderSetting("قص يمين", asset.sliceRight, 0f, 1f, { asset.sliceRight = it; onUpdateAsset(index, asset.copy()) }); SliderSetting("قص يسار", asset.sliceLeft, 0f, 1f, { asset.sliceLeft = it; onUpdateAsset(index, asset.copy()) })
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSetting(label: String, value: Float, start: Float, end: Float, onValueChange: (Float) -> Unit) {
    Column { Text("$label: ${String.format("%.2f", value)}", color = MaterialTheme.colorScheme.onSurface); Slider(value = value, onValueChange = onValueChange, valueRange = start..end) }
}

@Composable
fun ColorPickerRow(label: String, color: Color, onColorChange: (Color) -> Unit) {
    val colors = listOf(Color(0xFF000000), Color(0xFF333333), Color(0xFF666666), Color(0xFFFFFFFF), Color(0xFF8B5CF6), Color(0xFFE91E63))
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) { Box(modifier = Modifier.size(24.dp).background(color)); Spacer(modifier = Modifier.width(12.dp)); Text(label, color = MaterialTheme.colorScheme.onSurface) }
        if (expanded) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { colors.forEach { c -> Box(modifier = Modifier.size(28.dp).background(c).clickable { onColorChange(c); expanded = false }) } }
    }
}

@Composable
fun ExportScreen(onExport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("تصدير الثيم", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("حفظ ZIP") }
    }
}
