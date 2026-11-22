package com.example.tarotadmin.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tarotadmin.core.sync.AdminFtpManager
import com.example.tarotadmin.core.sync.DeleteResult
import com.example.tarotadmin.data.UserFolderItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(onUserClick: (String) -> Unit) {
    var users by remember { mutableStateOf<List<UserFolderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var userToDelete by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        val folderNames = AdminFtpManager.getUsersList()
        users = folderNames.map { folder ->
            val info = AdminFtpManager.getDeviceInfo(folder)
            val isSync = AdminFtpManager.getRemoteSyncState(folder)
            UserFolderItem(folder, info, isSync)
        }
        isLoading = false
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Удалить устройство?") },
            text = { Text("Все данные устройства (${userToDelete}) будут удалены с сервера безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    val folder = userToDelete
                    userToDelete = null
                    if (folder != null) {
                        scope.launch {
                            isLoading = true
                            when (val result = AdminFtpManager.deleteUserFolder(folder)) {
                                is DeleteResult.Success -> {
                                    users = users.filterNot { it.folderName == folder }
                                    message = "Устройство «$folder» удалено с сервера"
                                }
                                else -> message = "Не удалось удалить «$folder». ${result.describe()}"
                            }
                            isLoading = false
                            refreshTrigger++ // синхронизируем список с сервером
                        }
                    }
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Отмена") } }
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("ОК") } },
            text = { Text(text) }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0D0B14),
        topBar = {
            TopAppBar(
                title = { Text("Устройства", color = Color.White) },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Outlined.Refresh, "Обновить", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFA78BFA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    Card(
                        onClick = { onUserClick(user.folderName) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A142B))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(Color(0xFFA78BFA), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("📱", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.folderName, color = Color.White, fontWeight = FontWeight.Bold)

                                if (user.info != null) {
                                    Text("${user.info.manufacturer} ${user.info.model}", color = Color(0xFFE7C26B), style = MaterialTheme.typography.bodyMedium)
                                    Text("Android ${user.info.androidVersion} • v${user.info.appVersion}", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                                    Text("Синхр.: ${user.info.lastSync}", color = Color(0xFFA78BFA), style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Icon(
                                imageVector = if (user.isSyncEnabled) Icons.Outlined.Sync else Icons.Outlined.SyncDisabled,
                                contentDescription = null,
                                tint = if (user.isSyncEnabled) Color(0xFF57C48D) else Color(0xFFE5737F),
                                modifier = Modifier.padding(8.dp)
                            )

                            IconButton(onClick = { userToDelete = user.folderName }) {
                                Icon(Icons.Outlined.Delete, "Удалить", tint = Color(0xFFE5737F))
                            }
                        }
                    }
                }
            }
        }
    }
}