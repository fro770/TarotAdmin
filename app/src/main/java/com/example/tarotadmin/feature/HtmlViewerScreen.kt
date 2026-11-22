package com.example.tarotadmin.feature

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tarotadmin.core.sync.AdminFtpManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlViewerScreen(
    userFolder: String,
    subFolder: String, // "html_readings" или "html_matrices"
    id: String,
    title: String,
    onBack: () -> Unit
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        isLoading = true
        htmlContent = AdminFtpManager.downloadHtml(userFolder, subFolder, id)
        isLoading = false
    }

    // ✅ Сохраняем в локальную переменную, чтобы компилятор мог сделать smart cast
    val currentHtml = htmlContent

    Scaffold(
        containerColor = Color(0xFF0D0B14),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A142B))
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                isLoading -> CircularProgressIndicator(color = Color(0xFFA78BFA))
                currentHtml != null -> {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                setBackgroundColor(android.graphics.Color.parseColor("#0D0B14"))
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadDataWithBaseURL(null, currentHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> Text("Не удалось загрузить HTML файл", color = Color.White)
            }
        }
    }
}