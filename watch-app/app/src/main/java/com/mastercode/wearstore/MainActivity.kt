package com.mastercode.wearstore

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

// ← שנה לכתובת ה-Render שלך
const val SERVER_URL = "https://your-app.onrender.com"

data class App(
    val name: String,
    val filename: String,
    val size: String,
    val downloadUrl: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearStoreApp(this) }
    }
}

@Composable
fun WearStoreApp(context: Context) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<App>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var installingApp by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("$SERVER_URL/apps").readText()
                }
                val arr = JSONArray(json)
                apps = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    App(
                        name = obj.getString("name"),
                        filename = obj.getString("filename"),
                        size = obj.getString("size"),
                        downloadUrl = SERVER_URL + obj.getString("downloadUrl")
                    )
                }
            } catch (e: Exception) {
                error = "שגיאת חיבור"
            }
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        indicatorColor = Color(0xFF00E5A0),
                        trackColor = Color(0xFF1A1A1A),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("טוען...", color = Color(0xFF888888), fontSize = 11.sp)
                }
            }
            error != null -> {
                Text(error!!, color = Color(0xFFFF4D6D), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            apps.isEmpty() -> {
                Text("אין אפליקציות", color = Color(0xFF555555), fontSize = 12.sp)
            }
            else -> {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            "האפליקציות שלי",
                            color = Color(0xFF00E5A0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    items(apps.size) { i ->
                        val app = apps[i]
                        val isInstalling = installingApp == app.filename
                        AppCard(
                            app = app,
                            isInstalling = isInstalling,
                            onInstall = {
                                installingApp = app.filename
                                statusMsg = "מוריד ${app.name}..."
                                downloadAndInstall(context, app) {
                                    installingApp = null
                                    statusMsg = null
                                }
                            }
                        )
                    }
                    if (statusMsg != null) {
                        item {
                            Text(
                                statusMsg!!,
                                color = Color(0xFF888888),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppCard(app: App, isInstalling: Boolean, onInstall: () -> Unit) {
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF141416),
            endBackgroundColor = Color(0xFF141416)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(app.size, color = Color(0xFF555555), fontSize = 10.sp)
            }
            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    indicatorColor = Color(0xFF00E5A0),
                    trackColor = Color(0xFF1A1A1A),
                    strokeWidth = 2.dp
                )
            } else {
                Chip(
                    onClick = onInstall,
                    label = { Text("התקן", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF0F2E23),
                        contentColor = Color(0xFF00E5A0)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

fun downloadAndInstall(context: Context, app: App, onDone: () -> Unit) {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(app.downloadUrl))
        .setTitle(app.name)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, app.filename)
        .setMimeType("application/vnd.android.package-archive")

    val downloadId = dm.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                context.unregisterReceiver(this)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val uri = dm.getUriForDownloadedFile(downloadId)
                    val installIntent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(installIntent)
                }
                cursor.close()
                onDone()
            }
        }
    }
    context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
}
