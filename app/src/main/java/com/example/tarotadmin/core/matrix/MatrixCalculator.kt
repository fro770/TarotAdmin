package com.example.tarotadmin.core.matrix

// ✅ Общий класс для любых матриц (личной и совместимости)
data class MatrixPoint(val value: Int, val arcanaName: String)
data class MatrixData(
    val center: MatrixPoint, val left: MatrixPoint, val top: MatrixPoint, val right: MatrixPoint,
    val bottom: MatrixPoint, val topLeft: MatrixPoint, val topRight: MatrixPoint,
    val bottomRight: MatrixPoint, val bottomLeft: MatrixPoint
)

object MatrixCalculator {
    private val arcanaNames = listOf(
        "", "Шут", "Маг", "Верховная Жрица", "Императрица", "Император", "Иерофант", "Влюбленные",
        "Колесница", "Сила", "Отшельник", "Колесо Фортуны", "Справедливость", "Повешенный",
        "Смерть", "Умеренность", "Дьявол", "Башня", "Звезда", "Луна", "Солнце", "Суд", "Мир"
    )

    private fun point(value: Int): MatrixPoint {
        var v = value
        while (v > 22) v = v.toString().map { it.digitToInt() }.sum()
        if (v <= 0) v = 22
        return MatrixPoint(v, arcanaNames.getOrElse(v) { "Аркан $v" })
    }

    fun calculatePersonal(day: Int, month: Int, year: Int): MatrixData {
        val left = point(day)
        val top = point(month)
        val right = point(year.toString().map { it.digitToInt() }.sum())
        val bottom = point(left.value + top.value + right.value)
        val center = point(left.value + top.value + right.value + bottom.value)
        val topLeft = point(left.value + top.value)
        val topRight = point(top.value + right.value)
        val bottomRight = point(right.value + bottom.value)
        val bottomLeft = point(bottom.value + left.value)

        return MatrixData(center, left, top, right, bottom, topLeft, topRight, bottomRight, bottomLeft)
    }

    fun calculateCompatibility(day1: Int, month1: Int, year1: Int, day2: Int, month2: Int, year2: Int): MatrixData {
        val p1 = calculatePersonal(day1, month1, year1)
        val p2 = calculatePersonal(day2, month2, year2)

        return MatrixData(
            center = point(p1.center.value + p2.center.value),
            left = point(p1.left.value + p2.left.value),
            top = point(p1.top.value + p2.top.value),
            right = point(p1.right.value + p2.right.value),
            bottom = point(p1.bottom.value + p2.bottom.value),
            topLeft = point(p1.topLeft.value + p2.topLeft.value),
            topRight = point(p1.topRight.value + p2.topRight.value),
            bottomRight = point(p1.bottomRight.value + p2.bottomRight.value),
            bottomLeft = point(p1.bottomLeft.value + p2.bottomLeft.value)
        )
    }
}