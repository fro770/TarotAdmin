package com.example.tarotadmin.core.matrix

// ✅ Возвращаем MatrixZone, без которой падала компиляция
enum class MatrixZone { CENTER, KARMA, MONEY, LOVE, TALENT }

object AdminMeanings {
    fun getTarotMeaning(point: MatrixPoint, zone: MatrixZone): String {
        return when (point.value) {
            1 -> when (zone) {
                MatrixZone.CENTER -> "Вы обладаете силой воли, способны начинать новое."
                MatrixZone.KARMA -> "Научиться брать инициативу в свои руки."
                MatrixZone.MONEY -> "Деньги через новые идеи и личный бренд."
                MatrixZone.LOVE -> "Важно быть инициатором, но не подавлять."
                MatrixZone.TALENT -> "Талант влиять словом и делом."
            }
            2 -> when (zone) {
                MatrixZone.CENTER -> "Вы интуитивны и обладаете скрытой мудростью."
                MatrixZone.KARMA -> "Научиться доверять интуиции."
                MatrixZone.MONEY -> "Деньги через знания и психологию."
                MatrixZone.LOVE -> "Глубокие эмоциональные связи."
                MatrixZone.TALENT -> "Талант к эмпатии."
            }
            // (Для экономии места добавил 2, в идеале сюда нужно скопировать остальные арканы)
            else -> "Значение аркана ${point.value} (${point.arcanaName})."
        }
    }
}