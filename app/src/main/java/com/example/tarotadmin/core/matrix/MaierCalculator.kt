package com.example.tarotadmin.core.matrix

import kotlin.math.abs

data class MaierMatrix(
    val date: String,
    val mainArcana1: Int,
    val mainArcana2: Int,
    val hiddenArcana1: Int,
    val hiddenArcana2: Int,
    val frequencyMap: Map<Int, Int>
)

object MaierCalculator {

    private fun digits(n: Int): List<Int> {
        return abs(n).toString().map { it.digitToInt() }
    }

    private fun reduceNumber(num: Int): Int {
        var n = abs(num)
        while (n > 22) {
            n = n.toString().map { it.digitToInt() }.sum()
        }
        return n
    }

    fun calculate(day: Int, month: Int, year: Int): MaierMatrix {
        val yearStr = abs(year).toString().padStart(4, '0')
        val allDigits = mutableListOf<Int>()

        // 1. Исходная дата (день/месяц с ведущими нулями, год из 4 цифр)
        day.toString().padStart(2, '0').forEach { allDigits.add(it.digitToInt()) }
        month.toString().padStart(2, '0').forEach { allDigits.add(it.digitToInt()) }
        yearStr.forEach { allDigits.add(it.digitToInt()) }

        // 2. День + Месяц (раскладываем по цифрам)
        val daySum = digits(day).sum()
        val monthSum = digits(month).sum()
        val dayMonthTotal = daySum + monthSum

        allDigits.addAll(digits(daySum))
        allDigits.addAll(digits(monthSum))
        allDigits.addAll(digits(dayMonthTotal))

        // 3. Год (половины и их сумма — по цифрам)
        val y1 = yearStr.substring(0, 2).map { it.digitToInt() }.sum()
        val y2 = yearStr.substring(2, 4).map { it.digitToInt() }.sum()
        val yearTotal = y1 + y2

        allDigits.addAll(digits(y1))
        allDigits.addAll(digits(y2))
        allDigits.addAll(digits(yearTotal))

        // 4. Главные арканы
        var s = dayMonthTotal + yearTotal
        if (s > 22) {
            s = digits(dayMonthTotal).sum() + digits(yearTotal).sum()
        }
        val main1 = s
        val main2 = digits(main1).sum()

        allDigits.addAll(digits(main1))
        if (main2 != main1) {
            allDigits.addAll(digits(main2))
        }

        // 5. Скрытые арканы
        val hidden1 = 22 - main1
        val hidden2 = 22 - main2

        val sign = if (year < 0) "-" else ""
        val dateString = "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$sign$yearStr"

        return MaierMatrix(
            date = dateString,
            mainArcana1 = main1,
            mainArcana2 = main2,
            hiddenArcana1 = hidden1,
            hiddenArcana2 = hidden2,
            frequencyMap = allDigits.groupingBy { it }.eachCount()
        )
    }
}