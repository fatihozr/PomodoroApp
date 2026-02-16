package com.fatih.pomodoroapp1.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatih.pomodoroapp1.domain.model.DayData
import com.fatih.pomodoroapp1.domain.model.HistoricalEvent
import com.fatih.pomodoroapp1.ui.model.CalendarUiState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val BubbleColor = Color(0xFFE0E0E5)

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRefreshEvents: () -> Unit = {},
    onNextCarousel: () -> Unit = {},
    onPreviousCarousel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ODAK AKIŞI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        Text(
            text = "Kayıtlar ve Tarih",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading && uiState.historicalEvents.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        } else {
            CalendarCard(
                initialMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                workDays = uiState.workDays,
                onMonthChange = onMonthChange,
                onDateSelected = onDateSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = uiState.selectedDate,
                transitionSpec = {
                    (fadeIn() + slideInVertically { height -> height / 2 }) togetherWith
                            (fadeOut() + slideOutVertically { height -> -height / 2 })
                },
                label = "DayDetailsAnimation"
            ) { date ->
                if (date != null) {
                    SelectedDayDetails(
                        date = date,
                        dayData = uiState.workDays[date]
                    )
                }
            }

            if (uiState.selectedDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            HistoricalEventsCarousel(
                events = uiState.historicalEvents,
                currentPage = uiState.carouselPage,
                totalPages = uiState.totalCarouselPages,
                onRefresh = onRefreshEvents,
                onNext = onNextCarousel,
                onPrevious = onPreviousCarousel
            )
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CalendarCard(
    initialMonth: YearMonth,
    selectedDate: LocalDate?,
    workDays: Map<LocalDate, DayData>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val startIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = startIndex) { Int.MAX_VALUE }
    val scope = rememberCoroutineScope()

    val startMonthReference = remember { initialMonth }

    LaunchedEffect(pagerState.settledPage) {
        val pageOffset = pagerState.settledPage - startIndex
        val newMonth = startMonthReference.plusMonths(pageOffset.toLong())
        onMonthChange(newMonth)
    }

    val currentDisplayMonth by remember {
        derivedStateOf {
            val pageOffset = pagerState.currentPage - startIndex
            startMonthReference.plusMonths(pageOffset.toLong())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BubbleColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki Ay")
                }

                Crossfade(targetState = currentDisplayMonth, label = "MonthTitle") { month ->
                    Text(
                        text = month.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("tr"))
                        ).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki Ay")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Pz", "Pt", "Sa", "Ça", "Pe", "Cu", "Ct").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
                key = { pageIndex -> pageIndex }
            ) { page ->
                val pageOffset = page - startIndex
                val pageMonth = startMonthReference.plusMonths(pageOffset.toLong())

                CalendarGrid(
                    yearMonth = pageMonth,
                    selectedDate = selectedDate,
                    workDays = workDays,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    workDays: Map<LocalDate, DayData>,
    onDateSelected: (LocalDate) -> Unit
) {
    val calendarDays = remember(yearMonth) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

        val days = ArrayList<LocalDate?>()
        for (i in 0 until firstDayOfWeek) days.add(null)
        for (i in 1..daysInMonth) days.add(yearMonth.atDay(i))
        while (days.size < 42) days.add(null)
        days
    }

    Column {
        calendarDays.chunked(7).forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEach { date ->
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            hasWork = workDays.containsKey(date),
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasWork: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            hasWork -> Color.White // BEYAZ RENK - PEMBEDEKİ DEĞİŞİKLİK
            else -> Color.Transparent
        },
        label = "BgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            hasWork -> Color.Black // Beyaz üzerine siyah yazı
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "TextColor"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isSelected || hasWork) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SelectedDayDetails(
    date: LocalDate,
    dayData: DayData?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BubbleColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "${date.dayOfMonth} ${
                    date.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("tr"))
                        .replaceFirstChar { it.uppercase() }
                }",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Toplam Odak Süresi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${dayData?.totalHours ?: 0} saat ${dayData?.totalMinutes ?: 0} dakika",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tamamlanan Pomodoro",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dayData?.completedPomodoros ?: 0} Pomodoro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (dayData != null && dayData.completedPomodoros > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(minOf(dayData.completedPomodoros, 10)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalEventsCarousel(
    events: List<HistoricalEvent>,
    currentPage: Int,
    totalPages: Int,
    onRefresh: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BubbleColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarihte Bugün",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalPages > 0) {
                        Text(
                            text = "$currentPage/$totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = events,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn() togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            ) + fadeOut()
                },
                label = "CarouselAnimation"
            ) { currentEvents ->
                Column {
                    if (currentEvents.isNotEmpty()) {
                        currentEvents.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${event.year}: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Bu tarih için kayıtlı olay bulunmuyor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (totalPages > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Önceki",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "15 saniyede bir otomatik değişir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Sonraki",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}