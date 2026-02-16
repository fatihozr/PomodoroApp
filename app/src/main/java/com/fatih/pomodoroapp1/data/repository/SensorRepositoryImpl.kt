package com.fatih.pomodoroapp1.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.fatih.pomodoroapp1.domain.repository.SensorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SensorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorRepository {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Geliştirilmiş shake detection parametreleri
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastShakeResetTime = 0L

    // AYARLAR - Gyroscope hassasiyeti
    private val SHAKE_THRESHOLD = 14f // Eşik değeri (yüksek = daha az hassas)
    private val SHAKE_TIME_THRESHOLD = 800L // Minimum shake aralığı (ms) - 500'den 800'e çıkardık
    private val SHAKE_COUNT_RESET_TIME = 1500L // Shake sayacını sıfırlama süresi (ms)
    private val MIN_SHAKE_COUNT = 1 // En az kaç kez shake algılanmalı
    private val SHAKE_DURATION_THRESHOLD = 150L // Shake süresi minimum ne kadar olmalı (ms)

    private var shakeStartTime = 0L
    private var isShaking = false

    override fun observeShakeEvents(): Flow<Unit> = callbackFlow {
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Toplam ivmeyi hesapla (gravity'yi çıkar)
                val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                val currentTime = System.currentTimeMillis()

                // Shake sayacını belirli süre sonra sıfırla
                if (currentTime - lastShakeResetTime > SHAKE_COUNT_RESET_TIME) {
                    shakeCount = 0
                    lastShakeResetTime = currentTime
                }

                if (gForce > SHAKE_THRESHOLD) {
                    // Shake başlangıcını kaydet
                    if (!isShaking) {
                        shakeStartTime = currentTime
                        isShaking = true
                    }

                    // Minimum shake süresi kontrolü
                    val shakeDuration = currentTime - shakeStartTime

                    // Yeterli süre shake yapıldıysa ve minimum aralık geçtiyse
                    if (shakeDuration >= SHAKE_DURATION_THRESHOLD &&
                        currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD) {

                        shakeCount++
                        lastShakeTime = currentTime
                        lastShakeResetTime = currentTime

                        println("📳 Shake algılandı! Count: $shakeCount/$MIN_SHAKE_COUNT, gForce: ${"%.2f".format(gForce)}, duration: ${shakeDuration}ms")

                        // Yeterli sayıda shake algılandıysa event gönder
                        if (shakeCount >= MIN_SHAKE_COUNT) {
                            println("✅ Shake onaylandı! Timer toggle ediliyor...")
                            trySend(Unit)
                            shakeCount = 0 // Sayacı sıfırla
                        }
                    }
                } else {
                    // Shake bitti
                    if (isShaking) {
                        isShaking = false
                        val shakeDuration = currentTime - shakeStartTime
                        println("🛑 Shake durdu. Süre: ${shakeDuration}ms")
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun observePhoneOrientation(): Flow<Boolean> = callbackFlow {
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Telefon pozisyonunu Z-axis ile belirle
                // Z > 8: Telefon yüzü yukarı (normal pozisyon)
                // Z < -8: Telefon yüzü aşağı (ters çevrilmiş)
                // -8 < Z < 8: Telefon yan/dikey pozisyon

                val isFaceDown = z < -7.0f

                // Debug log
                println("🔍 Orientation: x=${"%.2f".format(x)}, y=${"%.2f".format(y)}, z=${"%.2f".format(z)}, isFaceDown=$isFaceDown")

                trySend(isFaceDown)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}