package com.fatih.pomodoroapp1.domain.repository

import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    /**
     * Telefon sallama olaylarını gözlemler
     * @return Unit emit eder her sallama tespit edildiğinde
     */
    fun observeShakeEvents(): Flow<Unit>

    /**
     * Telefon pozisyonunu gözlemler (yüzü aşağı mı?)
     * @return Boolean - true: telefon yüzü aşağı çevrilmiş (Z-axis negatif),
     *                   false: telefon normal pozisyonda veya yan/dikey
     */
    fun observePhoneOrientation(): Flow<Boolean>
}