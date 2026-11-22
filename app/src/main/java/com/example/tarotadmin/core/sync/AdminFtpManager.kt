package com.example.tarotadmin.core.sync

import android.util.Log
import com.example.tarotadmin.data.DeviceInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
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

    /** Создаёт и логинит клиент; бросает исключение при проблемах связи. */
    private fun connectAndLogin(): FTPClient {
        val client = FTPClient().apply {
            setConnectTimeout(10000)
            setDataTimeout(Duration.ofSeconds(30))
            setDefaultTimeout(30000)
        }
        client.connect(FTP_HOST, FTP_PORT)
        if (client.replyCode != FTPReply.WELCOME) {
            throw java.io.IOException("Неожиданный ответ сервера: ${client.replyString}")
        }
        if (!client.login(FTP_USER, FTP_PASS)) {
            throw java.io.IOException("Отказ в логине: ${client.replyString}")
        }
        client.enterLocalPassiveMode()
        return client
    }

    private fun closeQuietly(client: FTPClient?) {
        try { client?.logout() } catch (_: Exception) {}
        try { client?.disconnect() } catch (_: Exception) {}
    }

    suspend fun getUsersList(): List<String> = withContext(Dispatchers.IO) {
        val users = mutableListOf<String>()
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            var files = listDirectoryEntries(ftpClient, BASE_DIR)
            if (files.isEmpty()) files = listDirectoryEntries(ftpClient, "/")
            files.forEach { if (it.isDirectory && it.name != "." && it.name != "..") users.add(it.name) }
        } catch (e: Exception) { Log.e("AdminFtp", "Ошибка: ${e.message}") } finally {
            closeQuietly(ftpClient)
        }
        users
    }

    suspend fun getDeviceInfo(folder: String): DeviceInfo? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile(FtpPaths.join(BASE_DIR, "$folder/device_info.json"), out)) {
                return@withContext Gson().fromJson(out.toString("UTF-8"), DeviceInfo::class.java)
            }
        } catch (_: Exception) {} finally { closeQuietly(ftpClient) }
        null
    }

    suspend fun getRemoteSyncState(folder: String): Boolean = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile(FtpPaths.join(BASE_DIR, "$folder/admin_sync_state.json"), out)) {
                val map = Gson().fromJson(out.toString("UTF-8"), Map::class.java)
                return@withContext map["sync_enabled"] as? Boolean ?: true
            }
        } catch (_: Exception) {} finally { closeQuietly(ftpClient) }
        true // По умолчанию включено
    }

    suspend fun setRemoteSyncState(folder: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            val json = Gson().toJson(mapOf("sync_enabled" to enabled))
            ByteArrayInputStream(json.toByteArray()).use {
                ftpClient.storeFile(FtpPaths.join(BASE_DIR, "$folder/admin_sync_state.json"), it)
            }
        } catch (_: Exception) {} finally { closeQuietly(ftpClient) }
    }

    /**
     * Удаляет папку устройства со всем содержимым. Возвращает [DeleteResult],
     * чтобы UI мог показать реальную причину, если удалить не удалось
     * (раньше ошибка «глоталась» пустым catch и было непонятно, почему папка осталась).
     */
    suspend fun deleteUserFolder(folder: String): DeleteResult = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()

            val root = FtpPaths.join(BASE_DIR, folder)
            // Проверяем существование папки через смену рабочей директории (CWD):
            // это надёжнее, чем парсинг листинга на самодельных FTP-серверах (NAS).
            if (!ftpClient.changeWorkingDirectory(root)) {
                return@withContext DeleteResult.Success // папки уже нет — ничего удалять не нужно
            }

            val remaining = mutableListOf<String>()
            // Работаем с относительными путями от корня папки устройства:
            // некоторые серверы неверно разрешают абсолютные пути в DELE/RMD.
            ftpClient.changeWorkingDirectory(root)
            deleteContents(ftpClient, "", remaining)

            // Пробуем удалить саму папку, вернувшись на уровень выше.
            val parent = root.substringBeforeLast('/', "")
            ftpClient.changeWorkingDirectory(if (parent.isEmpty()) "/" else parent)
            val dirName = root.substringAfterLast('/')
            if (!ftpClient.removeDirectory(dirName) && !ftpClient.removeDirectory(root)) {
                remaining.add(folder)
            }

            if (remaining.isNotEmpty()) {
                DeleteResult.PartialError(
                    remaining = remaining.distinct(),
                    serverReply = runCatching { ftpClient.replyString }.getOrNull()
                )
            } else {
                DeleteResult.Success
            }
        } catch (e: Exception) {
            Log.e("AdminFtp", "deleteUserFolder($folder): ${e.message}", e)
            DeleteResult.ConnectionError(e.message ?: e.javaClass.simpleName)
        } finally {
            closeQuietly(ftpClient)
        }
    }

    /**
     * Рекурсивно удаляет содержимое текущей рабочей директории (+[relPath]).
     * Элементы, которые сервер отказался удалять, собираются в [remaining].
     */
    private fun deleteContents(client: FTPClient, relPath: String, remaining: MutableList<String>) {
        for (entry in listDirectoryEntries(client, relPath)) {
            val name = FtpPaths.cleanName(entry.name)
            if (name == "." || name == "..") continue
            val childRel = if (relPath.isEmpty()) name else "$relPath/$name"
            val ok = if (entry.isDirectory) {
                deleteContents(client, childRel, remaining)
                client.removeDirectory(childRel)
            } else {
                client.deleteFile(childRel)
            }
            if (!ok) remaining.add(childRel)
        }
    }

    /**
     * Листинг директории с двумя стратегиями:
     * 1) MLSD/LIST через FTPClient.listFiles;
     * 2) фолбэк на NLST + CWD-проверку типа, если парсер LIST не справился
     *    (частая проблема прошивок D-Link/ASUS NAS — имена с пробелами и кириллицей).
     * [path] — относительный путь от текущей рабочей директории ("" = текущая).
     */
    private fun listDirectoryEntries(client: FTPClient, path: String): List<FTPFile> {
        val parsed = runCatching { client.listFiles(path)?.toList() }.getOrNull()
            ?.filter { entry ->
                val n = FtpPaths.cleanName(entry.name)
                n.isNotEmpty() && n != "." && n != ".." && !n.contains('=') // защита от мусора MLSD-атрибутов
            }
            .orEmpty()
        if (parsed.isNotEmpty()) return parsed

        // Фолбэк: NLST возвращает только имена. Тип каждого элемента определяем
        // через CWD (директория → заход успешен), затем возвращаемся назад.
        val cwd = runCatching { client.printWorkingDirectory() }.getOrNull() ?: "/"
        val names = runCatching { client.listNames(path).toList() }.getOrDefault(emptyList())
        return names.mapNotNull { rawName ->
            val name = FtpPaths.cleanName(rawName.substringAfterLast('/'))
            if (name.isEmpty() || name == "." || name == "..") return@mapNotNull null
            val isDir = runCatching {
                client.changeWorkingDirectory(name).also { client.changeWorkingDirectory(cwd) }
            }.getOrDefault(false)
            FTPFile().apply {
                this.name = name
                type = if (isDir) FTPFile.DIRECTORY_TYPE else FTPFile.FILE_TYPE
            }
        }
    }

    suspend fun <T> downloadJson(folder: String, fileName: String, type: Type): T? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

            var remotePath = FtpPaths.join(BASE_DIR, "$folder/$fileName")
            val outputStream = ByteArrayOutputStream()
            var success = ftpClient.retrieveFile(remotePath, outputStream)

            if (!success) {
                remotePath = FtpPaths.join("", "$folder/$fileName")
                outputStream.reset()
                success = ftpClient.retrieveFile(remotePath, outputStream)
            }

            if (success) {
                return@withContext Gson().fromJson(outputStream.toString("UTF-8"), type)
            }
        } catch (_: Exception) {} finally { closeQuietly(ftpClient) }
        null
    }

    suspend fun downloadHtml(folder: String, subFolder: String, id: String): String? = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = connectAndLogin()
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
            val out = ByteArrayOutputStream()
            if (ftpClient.retrieveFile(FtpPaths.join(BASE_DIR, "$folder/$subFolder/$id.html"), out)) {
                return@withContext out.toString("UTF-8")
            }
        } catch (_: Exception) {} finally { closeQuietly(ftpClient) }
        null
    }
}