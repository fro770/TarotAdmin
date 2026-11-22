package com.example.tarotadmin.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tarotadmin.data.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    info: DeviceInfo?,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0D0B14),
        topBar = {
            TopAppBar(
                title = { Text("Инфо об устройстве", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (info == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Информация недоступна", color = Color.White)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard("Устройство", listOf(
                "Имя" to info.deviceName,
                "Производитель" to info.manufacturer,
                "Бренд" to info.brand,
                "Модель" to info.model,
                "Кодовое имя" to info.device,
                "Продукт" to info.product
            ))

            InfoCard("Система", listOf(
                "Android" to "${info.androidVersion} (API ${info.apiLevel})",
                "Локаль" to info.locale,
                "Версия приложения" to info.appVersion,
                "Последняя синхронизация" to info.lastSync
            ))

            InfoCard("Железо", listOf(
                "ОЗУ (RAM)" to "${info.totalRamMb} МБ",
                "Память" to "${info.totalStorageMb} МБ",
                "Экран" to info.screenResolution,
                "Плотность (DPI)" to "${info.screenDensity}"
            ))

            InfoCard("Идентификация", listOf(
                "ANDROID_ID" to info.deviceId
            ))

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(title: String, items: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A142B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            items.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                    Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}