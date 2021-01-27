package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File

interface ExposureNotificationApi {
    suspend fun isEnabled(): Boolean
    suspend fun start()
    suspend fun stop()
    suspend fun version(): Long?
    suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey>
    suspend fun provideDiagnosisKeys(files: List<File>)
    suspend fun getExposureWindows(): List<ExposureWindow>
    suspend fun getDiagnosisKeysDataMapping(): DiagnosisKeysDataMapping
    fun setDiagnosisKeysDataMapping(dataMapping: DiagnosisKeysDataMapping)
    suspend fun isAvailable(): Boolean
    suspend fun isRunningNormally(): Boolean
    fun deviceSupportsLocationlessScanning(): Boolean
}
