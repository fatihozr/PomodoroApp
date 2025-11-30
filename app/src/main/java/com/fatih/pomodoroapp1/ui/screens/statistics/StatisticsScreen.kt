package com.fatih.pomodoroapp1.ui.screens.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatih.pomodoroapp1.domain.model.StatisticsPeriod
import com.fatih.pomodoroapp1.ui.model.StatisticsUiState
import kotlinx.coroutines.delay

// Özel arka plan rengi
val CustomCardBackgroundColor = Color(0xFFE0E0E5)

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onPeriodChange: (StatisticsPeriod) -> Unit,
    onGoalUpdate: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val stats = uiState.statistics

    // --- BİLDİRİM STATE'LERİ ---
    var messageVisible by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationIcon by remember { mutableStateOf(Icons.Default.CheckCircle) }
    var notificationIconColor by remember { mutableStateOf(Color.Unspecified) }

    // Mesaj görünür olduğunda 2.5 saniye bekle ve kapat
    LaunchedEffect(messageVisible) {
        if (messageVisible) {
            delay(2500)
            messageVisible = false
        }
    }

    // Ana kapsayıcı Box
    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Başlık
            Text(
                text = "ODAK AKIŞI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = "İstatistikler",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading durumu
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Periyot seçici
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = onPeriodChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- ANİMASYON BAŞLANGICI ---
                AnimatedContent(
                    targetState = uiState.selectedPeriod,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "StatsAnimation"
                ) { targetPeriod ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Üst istatistik kartları
                        StatsOverview(
                            totalPomodoros = stats.totalPomodoros,
                            totalFocusHours = stats.totalFocusHours,
                            averageDailyMinutes = stats.averageDailyMinutes
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Odaklanma süresi grafiği
                        WeeklyFocusChart(data = stats.weeklyFocusData)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aktivite dağılımı
                        ActivityDistributionChart(data = stats.activityDistribution)

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- HEDEF KARTI ---
                        GoalCard(
                            period = targetPeriod,
                            goalPomodoros = stats.weeklyGoalPomodoros,
                            completedPomodoros = stats.weeklyCompletedPomodoros,
                            onGoalUpdate = { newGoal ->
                                // Önce veritabanına kaydet
                                onGoalUpdate(newGoal)

                                // Feedback Mantığı
                                if (stats.weeklyCompletedPomodoros > newGoal) {
                                    // Eğer tamamlanan > hedef ise AŞIM yapılmıştır
                                    notificationMessage = "Hedef AŞILDI! Harikasın 🏆"
                                    notificationIcon = Icons.Default.EmojiEvents // Kupa
                                    notificationIconColor = Color(0xFFD4AF37) // Altın Rengi
                                } else {
                                    // Normal kayıt
                                    notificationMessage = "Hedef Kaydedildi"
                                    notificationIcon = Icons.Default.CheckCircle
                                    notificationIconColor = Color.Black
                                }

                                messageVisible = true
                            }
                        )
                    }
                }
                // --- ANİMASYON BİTİŞİ ---
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- DİNAMİK BİLDİRİM (NOTIFICATION) ---
        AnimatedVisibility(
            visible = messageVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Surface(
                modifier = Modifier.padding(top = 16.dp),
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.medium,
                color = CustomCardBackgroundColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = notificationMessage,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = notificationIcon,
                        contentDescription = null,
                        tint = if (notificationIconColor != Color.Unspecified) notificationIconColor else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CustomCardBackgroundColor
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticsPeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    onClick = { onPeriodSelected(period) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = period.displayName,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsOverview(
    totalPomodoros: Int,
    totalFocusHours: Int,
    averageDailyMinutes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Toplam Pomodoro",
            value = totalPomodoros.toString(),
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Toplam Odak Süresi",
            value = "$totalFocusHours saat",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Ort. Günlük Süre",
            value = "$averageDailyMinutes dk",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CustomCardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(32.dp),
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyFocusChart(data: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CustomCardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Odaklanma Süresi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            WeeklyBarChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Int>,
    modifier: Modifier = Modifier
) {
    val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val maxValue = data.maxOrNull()?.toFloat() ?: 1f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { _, value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val heightFraction = if (maxValue > 0) value / maxValue else 0f
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ActivityDistributionChart(data: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CustomCardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Aktivite Dağılımı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DonutChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
private fun DonutChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF2C2C2C),
        Color(0xFF9EADB8),
        Color(0xFFE89B63)
    )

    val total = remember(data) { data.values.sum() }
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 30.dp.toPx()
                    val componentSize = size.minDimension
                    val arcSize = componentSize - strokeWidth
                    val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                    if (total == 0) {
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            topLeft = topLeftOffset,
                            size = Size(arcSize, arcSize)
                        )
                    } else {
                        var startAngle = -90f
                        data.values.forEachIndexed { index, value ->
                            val sweepAngle = (value.toFloat() / total) * 360f
                            val color = colors.getOrElse(index) { defaultPrimaryColor }

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth),
                                topLeft = topLeftOffset,
                                size = Size(arcSize, arcSize)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.entries.forEachIndexed { index, entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors.getOrElse(index) { MaterialTheme.colorScheme.primary })
                        )
                        Text(
                            text = "${entry.key} %${entry.value}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    period: StatisticsPeriod,
    goalPomodoros: Int,
    completedPomodoros: Int,
    onGoalUpdate: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf("") }

    val titleText = remember(period) {
        when {
            period.name.contains("WEEK", ignoreCase = true) -> "Haftalık Hedef"
            period.name.contains("MONTH", ignoreCase = true) -> "Aylık Hedef"
            period.name.contains("YEAR", ignoreCase = true) -> "Yıllık Hedef"
            else -> "Hedef"
        }
    }

    val isGoalAchieved = goalPomodoros > 0 && completedPomodoros >= goalPomodoros

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("$titleText Belirle") },
            text = {
                Column {
                    Text("Bu periyot için kaç Pomodoro hedefliyorsun?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempGoalInput,
                        onValueChange = { tempGoalInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Örn: 25") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newGoal = tempGoalInput.toIntOrNull() ?: goalPomodoros
                        onGoalUpdate(newGoal)
                        showDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                tempGoalInput = if (goalPomodoros > 0) goalPomodoros.toString() else ""
                showDialog = true
            },
        colors = CardDefaults.cardColors(
            containerColor = CustomCardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isGoalAchieved) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isGoalAchieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$completedPomodoros / $goalPomodoros",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Hedefi güncellemek için dokun",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val progress = if (goalPomodoros > 0) {
                completedPomodoros.toFloat() / goalPomodoros.toFloat()
            } else 0f

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}