package com.example.tarotadmin.core.sync

import android.util.Log
import com.example.tarotadmin.data.DeviceInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.time.Duration

object AdminFtpManager {
    private const val FTP_HOST = "dlink-320l-local-server.asuscomm.com"
    private const val FTP_PORT = 21
    private const val FTP_USER = "Tarot_admin"
    private const val FTP_PASS = "qWOim1EbY2ae"
    private const val BASE_DIR = "/TarotAssistant_DB/"

    suspend fun getUsersList(): List<String> = withContext(Dispatchers.IO) {
        val users = mutableListOf<String>()
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000); setDataTimeout(Duration.ofSeconds(15)) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            if (!ftpClient.login(FTP_USER, FTP_PASS)) return@withContext emptyList()
            ftpClient.enterLocalPassiveMode()
            var files = ftpClient.listFiles(BASE_DIR)
            if (files.isNullOrEmpty()) files = ftpClient.listFiles("/")
            files?.forEach { if (it.isDirectory && it.name != "." && it.name != "..") users.add(it.name) }
        } catch (e: Exception) { Log.e("AdminFtp", "Ошибка: ${e.message}") } finally {
            try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {}
        }
        users
    }

    suspend fun getDeviceInfo(folder: String): DeviceInfo? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile("$BASE_DIR$folder/device_info.json", out)) {
                return@withContext Gson().fromJson(out.toString("UTF-8"), DeviceInfo::class.java)
            }
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
        null
    }

    suspend fun getRemoteSyncState(folder: String): Boolean = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile("$BASE_DIR$folder/admin_sync_state.json", out)) {
                val map = Gson().fromJson(out.toString("UTF-8"), Map::class.java)
                return@withContext map["sync_enabled"] as? Boolean ?: true
            }
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
        true // По умолчанию включено
    }

    suspend fun setRemoteSyncState(folder: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            val json = Gson().toJson(mapOf("sync_enabled" to enabled))
            ByteArrayInputStream(json.toByteArray()).use {
                ftpClient.storeFile("$BASE_DIR$folder/admin_sync_state.json", it)
            }
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
    }

    suspend fun deleteUserFolder(folder: String) = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            val path = "$BASE_DIR$folder"
            deleteDirectory(ftpClient, path)
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
    }

    private fun deleteDirectory(client: FTPClient, path: String) {
        val files = client.listFiles(path) ?: return
        for (file in files) {
            val filePath = "$path/${file.name}"
            if (file.isDirectory) {
                deleteDirectory(client, filePath)
            } else {
                client.deleteFile(filePath)
            }
        }
        client.removeDirectory(path)
    }

    suspend fun <T> downloadJson(folder: String, fileName: String, type: Type): T? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

            var remotePath = "$BASE_DIR$folder/$fileName"
            val outputStream = ByteArrayOutputStream()
            var success = ftpClient.retrieveFile(remotePath, outputStream)

            if (!success) {
                remotePath = "/$folder/$fileName"
                outputStream.reset()
                success = ftpClient.retrieveFile(remotePath, outputStream)
            }

            if (success) {
                return@withContext Gson().fromJson(outputStream.toString("UTF-8"), type)
            }
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
        null
    }

    suspend fun downloadHtml(folder: String, subFolder: String, id: String): String? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = FTPClient().apply { setConnectTimeout(10000) }
            ftpClient.connect(FTP_HOST, FTP_PORT)
            ftpClient.login(FTP_USER, FTP_PASS)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile("$BASE_DIR$folder/$subFolder/$id.html", out)) {
                return@withContext out.toString("UTF-8")
            }
        } catch (_: Exception) {} finally { try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {} }
        null
    }
}