package com.example.tarotadmin.core.sync

/** Результат удаления папки устройства с FTP-сервера. */
sealed class DeleteResult {
    /** Папка успешно удалена (или её уже не существовало). */
    object Success : DeleteResult()

    /** Не удалось подключиться/залогиниться на сервер. */
    data class ConnectionError(val message: String) : DeleteResult()

    /** Папка не пуста и в ней остались элементы, которые сервер отказался удалять. */
    data class PartialError(
        val remaining: List<String>,
        val serverReply: String? = null
    ) : DeleteResult()

    fun describe(): String = when (this) {
        is Success -> "Папка удалена"
        is ConnectionError -> "Нет связи с FTP-сервером: $message"
        is PartialError -> {
            val list = remaining.joinToString(", ")
            "Сервер не дал удалить: $list" +
                (serverReply?.let { " (ответ сервера: $it)" } ?: "")
        }
    }
}
