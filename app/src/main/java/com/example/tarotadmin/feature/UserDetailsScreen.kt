package com.example.tarotadmin.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tarotadmin.core.sync.AdminFtpManager
import com.example.tarotadmin.data.ChatsResponse
import com.example.tarotadmin.data.DeviceInfo
import com.example.tarotadmin.data.FtpChatMessage
import com.example.tarotadmin.data.FtpLaunch
import com.example.tarotadmin.data.FtpMatrix
import com.example.tarotadmin.data.FtpReading
import com.example.tarotadmin.data.ReadingsResponse
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

// === ПЕРЕВОДЧИКИ ===
fun translateStatus(status: String): String = when (status) {
    "PENDING" -> "Ожидается"
    "DONE" -> "Завершён"
    "VERIFIED" -> "Проверен временем"
    "DRAFT" -> "Черновик"
    else -> status
}

fun translateMatrixType(type: String): String = when (type) {
    "personal" -> "Личная матрица"
    "compatibility" -> "Матрица отношений"
    "maier" -> "Матрица по Маеровки"
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    userFolder: String,
    onBack: () -> Unit,
    onOpenHtml: (String, String, String) -> Unit,
    onOpenMatrixGraphic: (String, String, String, String, String) -> Unit,
    onOpenDeviceInfo: (DeviceInfo?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Чаты", "Расклады", "Матрицы")
    var showLaunchesSheet by remember { mutableStateOf(false) }

    // ✅ Состояния для информации и статуса
    var isSyncEnabled by remember { mutableStateOf(true) }
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userFolder) {
        deviceInfo = AdminFtpManager.getDeviceInfo(userFolder)
        isSyncEnabled = AdminFtpManager.getRemoteSyncState(userFolder)
    }

    Scaffold(
        containerColor = Color(0xFF0D0B14),
        topBar = {
            TopAppBar(
                title = { Text(userFolder, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                actions = {
                    // ✅ Тумблер синхронизации
                    IconButton(onClick = {
                        scope.launch {
                            isSyncEnabled = !isSyncEnabled
                            AdminFtpManager.setRemoteSyncState(userFolder, isSyncEnabled)
                        }
                    }) {
                        Icon(
                            imageVector = if (isSyncEnabled) Icons.Outlined.Sync else Icons.Outlined.SyncDisabled,
                            contentDescription = "Синхронизация",
                            tint = if (isSyncEnabled) Color(0xFF57C48D) else Color(0xFFE5737F)
                        )
                    }
                    // ✅ Кнопка инфо
                    IconButton(onClick = { onOpenDeviceInfo(deviceInfo) }) {
                        Icon(Icons.Outlined.Info, "Инфо", tint = Color.White)
                    }
                    // ✅ Кнопка истории
                    IconButton(onClick = { showLaunchesSheet = true }) {
                        Icon(Icons.Outlined.History, "История входов", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFFA78BFA), divider = {}) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> ChatsTab(userFolder)
                1 -> ReadingsTab(userFolder, onOpenHtml)
                2 -> MatricesTab(userFolder, onOpenMatrixGraphic)
            }
        }
    }

    if (showLaunchesSheet) {
        ModalBottomSheet(onDismissRequest = { showLaunchesSheet = false }, containerColor = Color(0xFF1A142B)) {
            LaunchesSheet(userFolder)
        }
    }
}

// ==========================================
// ВКЛАДКА: ЧАТЫ
// ==========================================
@Composable
fun ChatsTab(userFolder: String) {
    var data by remember { mutableStateOf<ChatsResponse?>(null) }
    var expandedSessions by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(userFolder) {
        val type = object : TypeToken<ChatsResponse>() {}.type
        data = AdminFtpManager.downloadJson(userFolder, "chats.json", type)
    }

    data?.let { response ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(response.sessions) { session ->
                val isExpanded = session.id in expandedSessions
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A142B))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedSessions = if (isExpanded) expandedSessions - session.id else expandedSessions + session.id
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(session.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(session.createdAt, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            val msgs = response.messages.filter { it.sessionId == session.id }
                            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                msgs.forEach { msg -> ChatMessageCard(msg) }
                            }
                        }
                    }
                }
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFA78BFA)) }
}

@Composable
fun ChatMessageCard(msg: FtpChatMessage) {
    var isTextExpanded by remember { mutableStateOf(false) }
    val isAi = msg.role == "assistant"

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isAi) Color(0xFF251B3D) else Color(0xFF241C3E))) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (isAi) "🤖 ИИ" else "👤 Пользователь",
                color = if (isAi) Color(0xFFA78BFA) else Color(0xFFE7C26B),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            if (isAi) {
                SelectionContainer {
                    Text(
                        text = msg.text.replace("**", "").replace("*", ""),
                        color = Color.White, style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (isTextExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis, lineHeight = 20.sp
                    )
                }
                TextButton(onClick = { isTextExpanded = !isTextExpanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (isTextExpanded) "Свернуть" else "Развернуть", color = Color(0xFFA78BFA), fontSize = 12.sp)
                }
            } else {
                Text(msg.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ==========================================
// ВКЛАДКА: РАСКЛАДЫ
// ==========================================
@Composable
fun ReadingsTab(userFolder: String, onOpenHtml: (String, String, String) -> Unit) {
    var data by remember { mutableStateOf<ReadingsResponse?>(null) }
    LaunchedEffect(userFolder) {
        val type = object : TypeToken<ReadingsResponse>() {}.type
        data = AdminFtpManager.downloadJson(userFolder, "readings.json", type)
    }
    data?.let { response ->
        val rootReadings = response.readings.filter { it.parentReadingId.isBlank() }
        val childMap = response.readings.filter { it.parentReadingId.isNotBlank() }.groupBy { it.parentReadingId }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rootReadings) { reading ->
                ReadingCard(reading, childMap[reading.id] ?: emptyList(), onOpenHtml)
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFA78BFA)) }
}

@Composable
fun ReadingCard(reading: FtpReading, children: List<FtpReading>, onOpenHtml: (String, String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A142B))) {
        Column(Modifier.padding(16.dp)) {
            Text(reading.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Дата: ${reading.createdAt} | Статус: ${translateStatus(reading.status)}", color = Color(0xFFA78BFA), style = MaterialTheme.typography.bodySmall)

            if (reading.question.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Вопрос: ${reading.question}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) "Скрыть ИИ-отчёт" else "Показать ИИ-отчёт", color = Color(0xFFE7C26B), fontSize = 12.sp)
                }
                TextButton(onClick = { onOpenHtml("html_readings", reading.id, reading.title) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Открыть в HTML", color = Color(0xFFA78BFA), fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF251B3D)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ИИ-ОТЧЕТ", color = Color(0xFFE7C26B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(text = reading.aiReport.ifBlank { "Отчет отсутствует" }.replace("**", "").replace("*", ""), color = Color.White, lineHeight = 20.sp, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (children.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Уточняющие расклады:", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                children.forEach { child ->
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF241C3E))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(child.title, color = Color(0xFFE7C26B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Дата: ${child.createdAt} | Статус: ${translateStatus(child.status)}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { onOpenHtml("html_readings", child.id, child.title) }, contentPadding = PaddingValues(0.dp)) {
                                Text("Открыть уточнение", color = Color(0xFFA78BFA), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ВКЛАДКА: МАТРИЦЫ
// ==========================================
@Composable
fun MatricesTab(userFolder: String, onOpenMatrixGraphic: (String, String, String, String, String) -> Unit) {
    var data by remember { mutableStateOf<List<FtpMatrix>?>(null) }
    LaunchedEffect(userFolder) {
        val type = object : TypeToken<List<FtpMatrix>>() {}.type
        data = AdminFtpManager.downloadJson(userFolder, "matrices.json", type)
    }
    data?.let { matrices ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(matrices) { matrix ->
                MatrixCard(matrix, onOpenMatrixGraphic)
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFA78BFA)) }
}

@Composable
fun MatrixCard(matrix: FtpMatrix, onOpenMatrixGraphic: (String, String, String, String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A142B))) {
        Column(Modifier.padding(16.dp)) {
            Text(matrix.personName.ifBlank { "Без имени" }, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Тип: ${translateMatrixType(matrix.type)} | Дата: ${matrix.date1}", color = Color(0xFFE7C26B), style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) "Скрыть ИИ-отчёт" else "Показать ИИ-отчёт", color = Color(0xFFE7C26B), fontSize = 12.sp)
                }
                TextButton(onClick = {
                    onOpenMatrixGraphic(matrix.type, matrix.date1, matrix.date2, matrix.personName.ifBlank { "Матрица" }, matrix.aiReport)
                }, contentPadding = PaddingValues(0.dp)) {
                    Text("Посмотреть матрицу", color = Color(0xFFA78BFA), fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF251B3D)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ИИ-ОТЧЕТ", color = Color(0xFFE7C26B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(text = matrix.aiReport.ifBlank { "Отчет отсутствует" }.replace("**", "").replace("*", ""), color = Color.White, lineHeight = 20.sp, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ШТОРКА: ИСТОРИЯ ВХОДОВ
// ==========================================
@Composable
fun LaunchesSheet(userFolder: String) {
    var data by remember { mutableStateOf<List<FtpLaunch>?>(null) }
    LaunchedEffect(userFolder) {
        val type = object : TypeToken<List<FtpLaunch>>() {}.type
        data = AdminFtpManager.downloadJson(userFolder, "launches.json", type)
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("История входов", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(16.dp))

        data?.let { launches ->
            LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(launches) { launch ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF251B3D))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("📱", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Text("Заход в приложение", color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Text(launch.timestamp, color = Color(0xFFA78BFA), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFA78BFA))
        }
    }
}