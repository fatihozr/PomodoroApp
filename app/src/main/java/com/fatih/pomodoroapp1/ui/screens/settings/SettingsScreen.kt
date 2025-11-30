package com.fatih.pomodoroapp1.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fatih.pomodoroapp1.domain.model.Settings
import com.fatih.pomodoroapp1.ui.model.SettingsUiState
import kotlinx.coroutines.delay

private val BubbleColor = Color(0xFFE0E0E5)

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSettingsChange: (Settings) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    var messageVisible by remember { mutableStateOf(false) }
    var isSaveTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaving) {
        if (uiState.isSaving) {
            messageVisible = false
        }
    }

    LaunchedEffect(uiState.isSaving, uiState.saveSuccess, uiState.error) {
        if (!uiState.isSaving && (uiState.saveSuccess || uiState.error != null) && isSaveTriggered) {
            messageVisible = true
            delay(1000)
            messageVisible = false
            isSaveTriggered = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                text = "Pomodoro Ayarları",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = BubbleColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        InternalSettingItem(
                            title = "Pomodoro Süresi",
                            value = settings.pomodoroMinutes,
                            unit = "dakika",
                            valueRange = 1f..60f,
                            onValueChange = { onSettingsChange(settings.copy(pomodoroMinutes = it.toInt())) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        InternalSettingItem(
                            title = "Kısa Mola",
                            value = settings.shortBreakMinutes,
                            unit = "dakika",
                            valueRange = 1f..30f,
                            onValueChange = { onSettingsChange(settings.copy(shortBreakMinutes = it.toInt())) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        InternalSettingItem(
                            title = "Uzun Mola",
                            value = settings.longBreakMinutes,
                            unit = "dakika",
                            valueRange = 5f..60f,
                            onValueChange = { onSettingsChange(settings.copy(longBreakMinutes = it.toInt())) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        InternalSettingItem(
                            title = "Döngü Sayısı",
                            value = settings.pomodoroCount,
                            unit = "Pomodoro",
                            valueRange = 1f..10f,
                            onValueChange = { onSettingsChange(settings.copy(pomodoroCount = it.toInt())) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bildirim Sesleri",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(notificationsEnabled = it)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    isSaveTriggered = true
                    onSaveClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "KAYDET",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

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
                color = if (uiState.error != null) MaterialTheme.colorScheme.error else BubbleColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (uiState.error != null) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Kaydedildi",
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InternalSettingItem(
    title: String,
    value: Int,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val animatedSliderValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing),
        label = "SliderAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically(animationSpec = tween(150)) { height -> height } + fadeIn(animationSpec = tween(150))) togetherWith
                                (slideOutVertically(animationSpec = tween(150)) { height -> -height } + fadeOut(animationSpec = tween(150)))
                    } else {
                        (slideInVertically(animationSpec = tween(150)) { height -> -height } + fadeIn(animationSpec = tween(150))) togetherWith
                                (slideOutVertically(animationSpec = tween(150)) { height -> height } + fadeOut(animationSpec = tween(150)))
                    }
                },
                label = "CounterAnimation"
            ) { targetValue ->
                Text(
                    text = "$targetValue $unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Slider(
            value = animatedSliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
                colors = SliderDefaults.colors(
                inactiveTrackColor = Color(0xFFFFFFFF)  // Gri ton
                )
        )
    }
}