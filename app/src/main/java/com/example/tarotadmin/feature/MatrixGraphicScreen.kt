package com.example.tarotadmin.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tarotadmin.core.matrix.AdminMeanings
import com.example.tarotadmin.core.matrix.MaierCalculator
import com.example.tarotadmin.core.matrix.MaierMatrix
import com.example.tarotadmin.core.matrix.MatrixCalculator
import com.example.tarotadmin.core.matrix.MatrixPoint
import com.example.tarotadmin.core.matrix.MatrixZone
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixGraphicScreen(
    matrixType: String,
    date1: String,
    date2: String,
    title: String,
    aiReport: String,
    onBack: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val parts1 = date1.split(".")
    val d1 = parts1.getOrNull(0)?.toIntOrNull() ?: 1
    val m1 = parts1.getOrNull(1)?.toIntOrNull() ?: 1
    val y1 = parts1.getOrNull(2)?.toIntOrNull() ?: 2000

    val parts2 = date2.split(".")
    val d2 = parts2.getOrNull(0)?.toIntOrNull() ?: 1
    val m2 = parts2.getOrNull(1)?.toIntOrNull() ?: 1
    val y2 = parts2.getOrNull(2)?.toIntOrNull() ?: 2000

    val isMaier = matrixType == "maier"

    val maierMatrix = remember { if (isMaier) MaierCalculator.calculate(d1, m1, y1) else null }
    val tarotMatrix = remember { if (!isMaier) (if (matrixType == "compatibility") MatrixCalculator.calculateCompatibility(d1, m1, y1, d2, m2, y2) else MatrixCalculator.calculatePersonal(d1, m1, y1)) else null }

    Scaffold(
        containerColor = Color(0xFF0D0B14),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Дата: $date1", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA78BFA))
            }

            if (isMaier && maierMatrix != null) {
                // Отрисовка Х-креста
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF1A142B), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val baseR = size.minDimension * 0.38f
                        val random = Random(maierMatrix.mainArcana1 * 100 + maierMatrix.hiddenArcana2)

                        val pts = listOf(
                            Offset((random.nextFloat() * 10f - 5f), -baseR + (random.nextFloat() * 10f - 5f)),
                            Offset((random.nextFloat() * 10f - 5f), baseR + (random.nextFloat() * 10f - 5f)),
                            Offset(-baseR * 0.85f + (random.nextFloat() * 10f - 5f), -baseR * 0.85f + (random.nextFloat() * 10f - 5f)),
                            Offset(baseR * 0.85f + (random.nextFloat() * 10f - 5f), baseR * 0.85f + (random.nextFloat() * 10f - 5f)),
                            Offset(baseR * 0.85f + (random.nextFloat() * 10f - 5f), -baseR * 0.85f + (random.nextFloat() * 10f - 5f)),
                            Offset(-baseR * 0.85f + (random.nextFloat() * 10f - 5f), baseR * 0.85f + (random.nextFloat() * 10f - 5f))
                        )

                        val r = size.minDimension * 0.38f
                        val pTop = Offset(cx + pts[0].x * (r/baseR), cy + pts[0].y * (r/baseR))
                        val pBottom = Offset(cx + pts[1].x * (r/baseR), cy + pts[1].y * (r/baseR))
                        val pTopLeft = Offset(cx + pts[2].x * (r/baseR), cy + pts[2].y * (r/baseR))
                        val pBottomRight = Offset(cx + pts[3].x * (r/baseR), cy + pts[3].y * (r/baseR))
                        val pTopRight = Offset(cx + pts[4].x * (r/baseR), cy + pts[4].y * (r/baseR))
                        val pBottomLeft = Offset(cx + pts[5].x * (r/baseR), cy + pts[5].y * (r/baseR))

                        val color = Color(0xFFA78BFA).copy(alpha = 0.6f)

                        drawLine(color, pTop, pBottom, strokeWidth = 2.5f)
                        drawLine(color, pTopLeft, pBottomRight, strokeWidth = 2.5f)
                        drawLine(color, pTopRight, pBottomLeft, strokeWidth = 2.5f)

                        fun centroid(p1: Offset, p2: Offset, p3: Offset): Offset {
                            return Offset((p1.x + p2.x + p3.x) / 3f, (p1.y + p2.y + p3.y) / 3f)
                        }
                        val center = Offset(cx, cy)
                        val posLeft = centroid(center, pTopLeft, pBottomLeft)
                        val posBottomLeft = centroid(center, pBottomLeft, pBottom)
                        val posRight = centroid(center, pTopRight, pBottomRight)
                        val posBottomRight = centroid(center, pBottomRight, pBottom)

                        val rotTL = random.nextFloat() * -15f - 5f
                        val rotBL = random.nextFloat() * -15f - 5f
                        val rotTR = random.nextFloat() * 15f + 5f
                        val rotBR = random.nextFloat() * 15f + 5f

                        drawRotatedText(textMeasurer, maierMatrix.mainArcana1.toString(), posLeft, rotTL, Color.White)
                        drawRotatedText(textMeasurer, maierMatrix.mainArcana2.toString(), posBottomLeft, rotBL, Color.White)
                        drawRotatedText(textMeasurer, maierMatrix.hiddenArcana1.toString(), posRight, rotTR, Color.White)
                        drawRotatedText(textMeasurer, maierMatrix.hiddenArcana2.toString(), posBottomRight, rotBR, Color.White)
                    }
                }

                AdminMaierGrid(maierMatrix)

            } else if (tarotMatrix != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF1A142B), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension * 0.35f
                        val center = Offset(cx, cy)
                        val angles = listOf(270f, 315f, 0f, 45f, 90f, 135f, 180f, 225f)
                        val pts = angles.map { a ->
                            val rad = Math.toRadians(a.toDouble())
                            Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat())
                        }

                        pts.forEach { drawLine(Color(0xFFA78BFA).copy(alpha = 0.5f), center, it, strokeWidth = 2f) }
                        for (i in pts.indices) drawLine(Color(0xFFA78BFA).copy(alpha = 0.3f), pts[i], pts[(i + 1) % pts.size], strokeWidth = 1.5f)

                        drawNode(textMeasurer, center, tarotMatrix.center.value.toString(), 40f, Color(0xFFE7C26B), Color(0xFF1A142B), isCenter = true)
                        drawNode(textMeasurer, pts[0], tarotMatrix.top.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[1], tarotMatrix.topRight.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[2], tarotMatrix.right.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[3], tarotMatrix.bottomRight.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[4], tarotMatrix.bottom.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[5], tarotMatrix.bottomLeft.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[6], tarotMatrix.left.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                        drawNode(textMeasurer, pts[7], tarotMatrix.topLeft.value.toString(), 28f, Color(0xFFA78BFA), Color(0xFF1A142B))
                    }
                }

                Text("Детализация по зонам", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                AdminTarotMeaningCard("Зона комфорта", tarotMatrix.center, MatrixZone.CENTER)
                AdminTarotMeaningCard("Кармический урок", tarotMatrix.bottom, MatrixZone.KARMA)
                AdminTarotMeaningCard("Денежный канал", tarotMatrix.bottomRight, MatrixZone.MONEY)
                AdminTarotMeaningCard("Канал отношений", tarotMatrix.topRight, MatrixZone.LOVE)
                AdminTarotMeaningCard("Внутренняя опора", tarotMatrix.bottomLeft, MatrixZone.TALENT)
            }

            // Вывод ИИ-отчета
            if (aiReport.isNotBlank()) {
                Text("Анализ от ИИ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF251B3D))) {
                    Column(Modifier.padding(16.dp)) {
                        SelectionContainer {
                            Text(
                                text = aiReport.replace("**", "").replace("*", ""),
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AdminMaierGrid(matrix: MaierMatrix) {
    val borderColor = Color(0xFFA78BFA).copy(alpha = 0.5f)
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(Color(0xFF1A142B), RoundedCornerShape(16.dp))
    ) {
        for (row in 0..2) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 8) {
                            // ✅ Правильное отображение 9-й ячейки с центрированием в треугольниках
                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalDivider(
                                    modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = -45f },
                                    color = borderColor
                                )

                                Column(modifier = Modifier.fillMaxSize()) {
                                    val c8 = matrix.frequencyMap[8] ?: 0
                                    val c9 = matrix.frequencyMap[9] ?: 0
                                    val text8 = if (c8 > 0) "8".repeat(c8) else "—"
                                    val text9 = if (c9 > 0) "9".repeat(c9) else "—"

                                    // ✅ Шрифт немного крупнее
                                    val size8 = when { c8 >= 5 -> 12.sp; c8 >= 3 -> 14.sp; else -> 18.sp }
                                    val size9 = when { c9 >= 5 -> 12.sp; c9 >= 3 -> 14.sp; else -> 18.sp }

                                    // Верхний треугольник
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(text = text8, color = Color(0xFFE7C26B), fontSize = size8, fontWeight = FontWeight.Bold)
                                    }
                                    // Нижний треугольник
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(text = text9, color = Color(0xFFE7C26B), fontSize = size9, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            val count = matrix.frequencyMap[index] ?: 0
                            val text = if (count > 0) index.toString().repeat(count) else "—"

                            val fontSize = when {
                                count >= 7 -> 11.sp
                                count >= 5 -> 13.sp
                                count >= 3 -> 16.sp
                                else -> 20.sp
                            }
                            Text(
                                text = text,
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE7C26B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTarotMeaningCard(title: String, point: MatrixPoint, zone: MatrixZone) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color(0xFFA78BFA), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("${point.value}", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(point.arcanaName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFFE7C26B))
            Spacer(Modifier.height(4.dp))
            Text(AdminMeanings.getTarotMeaning(point, zone), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), lineHeight = 20.sp)
        }
    }
}

private fun DrawScope.drawNode(
    textMeasurer: TextMeasurer, center: Offset, text: String, radius: Float, borderColor: Color, bgColor: Color, isCenter: Boolean = false
) {
    drawCircle(bgColor, radius, center)
    drawCircle(borderColor, radius, center, style = Stroke(width = if (isCenter) 3f else 2f))
    val textLayout = textMeasurer.measure(
        text = AnnotatedString(text), style = TextStyle(color = if (isCenter) bgColor else Color.White, fontSize = if (isCenter) 20.sp else 16.sp, fontWeight = FontWeight.Bold)
    )
    drawText(textLayoutResult = textLayout, topLeft = Offset(center.x - textLayout.size.width / 2f, center.y - textLayout.size.height / 2f))
}

private fun DrawScope.drawRotatedText(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    angle: Float,
    color: Color,
    fontSize: TextUnit = 20.sp
) {
    val style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
    val layout = textMeasurer.measure(AnnotatedString(text), style = style)

    rotate(degrees = angle, pivot = center) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
        )
    }
}