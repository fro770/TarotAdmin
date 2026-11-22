package com.example.tarotadmin.data

// Модель для device_info.json (Расширенная)
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val device: String = "",
    val product: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val totalRamMb: Long = 0,
    val totalStorageMb: Long = 0,
    val screenResolution: String = "",
    val screenDensity: Int = 0,
    val locale: String = "",
    val appVersion: String = "",
    val lastSync: String = ""
)

// Модели для остальных файлов
data class FtpLaunch(val id: String, val timestamp: String)
data class FtpChatSession(val id: String, val title: String, val createdAt: String)
data class FtpChatMessage(val id: String, val sessionId: String, val role: String, val text: String, val imageUri: String, val createdAt: String)
data class ChatsResponse(val sessions: List<FtpChatSession>, val messages: List<FtpChatMessage>)

data class FtpReading(
    val id: String, val title: String, val category: String, val question: String,
    val status: String, val createdAt: String, val updatedAt: String,
    val parentReadingId: String, val aiReport: String, val note: String
)
data class FtpReadingCard(val id: String, val readingId: String, val cardId: String, val positionIndex: Int, val positionTitle: String, val isReversed: Boolean)
data class ReadingsResponse(val readings: List<FtpReading>, val cards: List<FtpReadingCard>)

data class FtpMatrix(
    val id: String, val type: String, val personName: String,
    val date1: String, val date2: String, val aiReport: String, val createdAt: String
)

// Вспомогательная модель для списка юзеров
data class UserFolderItem(
    val folderName: String,
    val info: DeviceInfo?,
    val isSyncEnabled: Boolean
)