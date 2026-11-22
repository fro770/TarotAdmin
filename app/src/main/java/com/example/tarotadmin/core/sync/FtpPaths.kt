package com.example.tarotadmin.core.sync

/**
 * Утилиты для работы с путями FTP.
 *
 * Важно: в FTP-командах (DELE / RMD / MLSD ...) аргументом является один путь,
 * который сервер парсит целиком. Если имя папки/файла содержит пробелы или
 * спецсимволы, «наивная» склейка строк приводит к ошибкам вида
 * «No such file or directory», и удаление папок устройства не работает.
 * Поэтому пути нормализуются и экранируются перед отправкой на сервер.
 */
object FtpPaths {

    /** Склеивает базовый путь и относительный путь, убирая лишние слэши на стыке. */
    fun join(base: String, relative: String): String {
        val b = base.trimEnd('/')
        val r = relative.trim().trimStart('/')
        return if (r.isEmpty()) b else "$b/$r"
    }

    /** Разбивает вывод LIST/NLST на отдельные имена (по строкам), отбрасывая пустые. */
    fun splitNames(raw: String?): List<String> =
        raw?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    /**
     * Экранирует путь для передачи в FTP-команду.
     * Кавычки удваиваются, обратные слеши оставляются как есть — большинство
     * серверов (в т.ч. proftpd/pure-ftpd на NAS) принимают путь буквально.
     */
    fun quote(path: String): String = path.replace("\"", "\"\"")

    /** Приводит имя элемента из листинга к безопасному виду (без завершающих слэш/пробелов). */
    fun cleanName(name: String): String = name.trim().trimEnd('/')
}
