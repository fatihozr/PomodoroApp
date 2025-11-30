package com.fatih.pomodoroapp1.ui.screens.timer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatih.pomodoroapp1.domain.model.TimerPhase
import com.fatih.pomodoroapp1.ui.model.TimerUiState

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onPlayPauseClick: () -> Unit,
    onSkipClick: () -> Unit,
    onRestartClick: () -> Unit,
    onToggleShakeSensor: () -> Unit,
    onToggleProximitySensor: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🔥 Sensor'ler ViewModel'de sürekli çalışıyor - arka planda da aktif

    Column(
        modifier = modifier
            .fillMaxSize()
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
            text = when (uiState.phase) {
                TimerPhase.POMODORO -> "Odaklanma Zamanı"
                TimerPhase.SHORT_BREAK -> "Kısa Mola"
                TimerPhase.LONG_BREAK -> "Uzun Mola"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sensör Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SensorButton(
                icon = Icons.Outlined.Vibration,
                label = "Shake",
                isEnabled = uiState.isShakeSensorEnabled,
                onClick = onToggleShakeSensor
            )

            SensorButton(
                icon = Icons.Default.PhoneAndroid,
                label = "Proximity",
                isEnabled = uiState.isProximitySensorEnabled,
                onClick = onToggleProximitySensor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loading veya Error durumu
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(280.dp)
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                CircularTimer(
                    timeRemaining = uiState.timeRemainingSeconds,
                    totalTime = uiState.totalTimeSeconds,
                    modifier = Modifier.size(280.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pomodoro göstergesi
        Text(
            text = "${uiState.currentPomodoro}/${uiState.totalPomodoros} Pomodoro",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Kontrol butonları
        TimerControls(
            isPaused = uiState.isPaused,
            onPlayPauseClick = onPlayPauseClick,
            onSkipClick = onSkipClick,
            onRestartClick = onRestartClick
        )
    }
}

@Composable
private fun SensorButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        onClick = onClick,
        tonalElevation = if (isEnabled) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (isEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TimerControls(
    isPaused: Boolean,
    onPlayPauseClick: () -> Unit,
    onSkipClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    // --- BUTON ANİMASYONLARI İÇİN STATE'LER ---

    // 1. Skip Butonu
    val skipInteractionSource = remember { MutableInteractionSource() }
    val isSkipPressed by skipInteractionSource.collectIsPressedAsState()
    val skipScale by animateFloatAsState(
        targetValue = if (isSkipPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SkipScale"
    )

    // 2. Play/Pause Butonu
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "PlayScale"
    )

    // 3. Restart Butonu
    val restartInteractionSource = remember { MutableInteractionSource() }
    val isRestartPressed by restartInteractionSource.collectIsPressedAsState()
    val restartScale by animateFloatAsState(
        targetValue = if (isRestartPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "RestartScale"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SKIP BUTONU (ARTIK YUVARLAK)
        FilledTonalButton(
            onClick = onSkipClick,
            modifier = Modifier
                .size(80.dp)
                .scale(skipScale),
            shape = CircleShape, // --- DEĞİŞİKLİK BURADA: Yuvarlak yapıldı ---
            interactionSource = skipInteractionSource,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("Skip", fontWeight = FontWeight.Bold)
        }

        // PLAY/PAUSE BUTONU (EN BÜYÜK)
        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(100.dp)
                .scale(playScale),
            interactionSource = playInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Crossfade(targetState = isPaused, label = "IconFade") { paused ->
                Icon(
                    imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (paused) "Play" else "Pause",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // RESTART BUTONU
        FilledTonalIconButton(
            onClick = onRestartClick,
            modifier = Modifier
                .size(80.dp)
                .scale(restartScale),
            interactionSource = restartInteractionSource,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart"
            )
        }
    }
}

@Composable
private fun CircularTimer(
    timeRemaining: Int,
    totalTime: Int,
    modifier: Modifier = Modifier
) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val progress = if (totalTime > 0) {
        timeRemaining.toFloat() / totalTime.toFloat()
    } else 0f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()

            // Arka plan çemberi
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = strokeWidth)
            )

            // İlerleme çemberi
            drawArc(
                color = Color.Black,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}