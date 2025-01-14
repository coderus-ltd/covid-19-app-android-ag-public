/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.tinder.StateMachine
import uk.nhs.covid19.config.Configurations
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.ExposureApplication
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationChecker
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.MockRandomNonRiskyExposureWindowsLimiter
import uk.nhs.nhsx.covid19.android.app.packagemanager.MockPackageManager
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.permissions.MockPermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.MockBarcodeDetectorBuilder
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.MockAnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockEpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.additionalInterceptors
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.state.Event
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.SideEffect
import uk.nhs.nhsx.covid19.android.app.status.DateChangeBroadcastReceiver
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.util.AndroidStrongBoxSupport
import uk.nhs.nhsx.covid19.android.app.util.EncryptedSharedPreferencesUtils
import uk.nhs.nhsx.covid19.android.app.util.EncryptedStorage
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import uk.nhs.nhsx.covid19.android.app.util.MockUUIDGenerator
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryChecker
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationRetryStorage
import uk.nhs.nhsx.covid19.android.app.util.getPrivateProperty
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

const val AWAIT_AT_MOST_SECONDS: Long = 10

class TestApplicationContext {

    val app: ExposureApplication = ApplicationProvider.getApplicationContext()
    val riskyVenuesApi = MockRiskyVenuesApi()

    val virologyTestingApi = MockVirologyTestingApi()

    val isolationPaymentApi = MockIsolationPaymentApi()

    val questionnaireApi = MockQuestionnaireApi()

    val keysSubmissionApi = MockKeysSubmissionApi()

    val analyticsApi = MockAnalyticsApi()

    val updateManager = TestUpdateManager()

    val permissionsManager = MockPermissionsManager()

    val packageManager = MockPackageManager()

    val barcodeDetectorProvider = MockBarcodeDetectorBuilder()

    val epidemiologyDataApi = MockEpidemiologyDataApi()

    val randomNonRiskyExposureWindowsLimiter = MockRandomNonRiskyExposureWindowsLimiter()

    val uuidGenerator = MockUUIDGenerator()

    val clock = MockClock()

    internal val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val exposureNotificationApi = MockExposureNotificationApi(clock)

    private val bluetoothStateProvider = TestBluetoothStateProvider()

    private val locationStateProvider = TestLocationStateProvider()

    private val batteryOptimizationChecker = TestBatteryOptimizationChecker()

    private val encryptionUtils = EncryptionUtils(AndroidStrongBoxSupport)
    private val encryptedSharedPreferencesUtils = EncryptedSharedPreferencesUtils(encryptionUtils)
    internal val encryptedStorage = EncryptedStorage.from(
        app,
        StrongBoxMigrationRetryChecker(
            StrongBoxMigrationRetryStorage(
                encryptedSharedPreferencesUtils.createGenericEncryptedSharedPreferences(
                    app,
                    encryptionUtils.getDefaultMasterKey(),
                    SharedPrefsDelegate.migrationSharedPreferencesFileName
                )
            )
        ),
        encryptionUtils
    )

    private val signatureKey = SignatureKey(
        id = "3",
        pemRepresentation =
            """
            -----BEGIN PUBLIC KEY-----
            MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9
            q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==
            -----END PUBLIC KEY-----
            """.trimIndent()
    )

    private val component: TestAppComponent = DaggerTestAppComponent.builder()
        .appModule(
            AppModule(
                app,
                exposureNotificationApi,
                bluetoothStateProvider,
                locationStateProvider,
                encryptedStorage.sharedPreferences,
                encryptedStorage.encryptedFile,
                signatureKey,
                updateManager,
                batteryOptimizationChecker,
                permissionsManager,
                packageManager,
                barcodeDetectorProvider,
                randomNonRiskyExposureWindowsLimiter,
                uuidGenerator,
                clock,
                DateChangeBroadcastReceiver()
            )
        )
        .networkModule(
            NetworkModule(
                Configurations.qa,
                additionalInterceptors
            )
        )
        .managedApiModule(
            ManagedApiModule(
                riskyVenuesApi,
                virologyTestingApi,
                questionnaireApi,
                isolationPaymentApi,
                keysSubmissionApi,
                analyticsApi,
                epidemiologyDataApi
            )
        )
        .build()

    init {
        app.appComponent = component
    }

    private fun closeNotificationPanel() {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        app.baseContext.sendBroadcast(it)
    }

    fun reset() {
        WorkManager.getInstance(app).cancelAllWork()

        encryptedStorage.sharedPreferences.edit(commit = true) { clear() }

        setExposureNotificationsEnabled(true)
        exposureNotificationApi.setDeviceSupportsLocationlessScanning(false)
        setOnboardingCompleted(true)
        setBluetoothEnabled(true)
        setLocationEnabled(true)
        setPolicyUpdateAccepted(true)
        FeatureFlagTestHelper.clearFeatureFlags()
        closeNotificationPanel()

        component.provideIsolationStateMachine().reset()
    }

    fun setBluetoothEnabled(isEnabled: Boolean) {
        bluetoothStateProvider.bluetoothStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setLocationEnabled(isEnabled: Boolean) {
        locationStateProvider.locationStateMutable.postValue(if (isEnabled) ENABLED else DISABLED)
    }

    fun setExposureNotificationsEnabled(isEnabled: Boolean) {
        exposureNotificationApi.setEnabled(isEnabled)
    }

    fun getExposureNotificationApi(): MockExposureNotificationApi {
        return exposureNotificationApi
    }

    fun setPostCode(postCode: String?) {
        component.getPostCodeProvider().value = postCode
    }

    fun setLocalAuthority(localAuthority: String?) {
        component.getLocalAuthorityProvider().value = localAuthority
    }

    fun setIsolationPaymentToken(token: String?) {
        component.getIsolationPaymentTokenStateProvider().tokenState = if (token != null) {
            IsolationPaymentTokenState.Token(token)
        } else {
            IsolationPaymentTokenState.Unresolved
        }
    }

    fun getSubmitAnalyticsAlarmController() = component.getSubmitAnalyticsAlarmController()

    fun getUserInbox() = component.getUserInbox()

    fun getUnacknowledgedTestResultsProvider() = component.getUnacknowledgedTestResultsProvider()

    fun getTestOrderingTokensProvider() = component.getTestOrderingTokensProvider()

    fun getKeySharingInfoProvider() = component.getKeySharingInfoProvider()

    fun setState(state: IsolationState) {
        val ref = component.provideIsolationStateMachine()
            .stateMachine
            .getPrivateProperty<StateMachine<IsolationState, Event, SideEffect>, AtomicReference<IsolationState>>(
                "stateRef"
            )
        ref?.set(state)
    }

    fun getCurrentState(): IsolationState =
        component.provideIsolationStateMachine().readState()

    fun getCurrentLogicalState(): IsolationLogicalState =
        component.provideIsolationStateMachine().readLogicalState()

    fun getExposureCircuitBreakerInfoProvider() =
        component.getExposureCircuitBreakerInfoProvider()

    fun getVisitedVenuesStorage(): VisitedVenuesStorage {
        return component.provideVisitedVenuesStorage()
    }

    fun getDownloadAndProcessRiskyVenues(): DownloadAndProcessRiskyVenues {
        return component.getDownloadAndProcessRiskyVenues()
    }

    fun getDownloadVirologyTestResultWork(): DownloadVirologyTestResultWork {
        return component.getDownloadVirologyTestResultWork()
    }

    fun temporaryExposureKeyHistoryWasCalled() =
        exposureNotificationApi.temporaryExposureKeyHistoryWasCalled()

    fun getPeriodicTasks(): PeriodicTasks {
        return component.providePeriodicTasks()
    }

    fun getDisplayStateExpirationNotification() =
        component.provideDisplayStateExpirationNotification()

    fun getIsolationConfigurationProvider() =
        component.getIsolationConfigurationProvider()

    fun setPolicyUpdateAccepted(accepted: Boolean) {
        component.getPolicyUpdateStorage().value = if (accepted) Int.MAX_VALUE.toString() else null
    }

    fun setLocale(languageCode: String?) {
        component.provideApplicationLocaleProvider().languageCode = languageCode
        updateResources()
    }

    private fun updateResources() {
        val locale = component.provideApplicationLocaleProvider().getLocale()
        Locale.setDefault(locale)
        val res: Resources = app.baseContext.resources
        val config = Configuration(res.configuration)
        config.locale = locale
        res.updateConfiguration(config, res.displayMetrics)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        component.provideOnboardingCompleted().value = completed
    }

    fun setAppAvailability(appAvailability: AppAvailabilityResponse) {
        component.getAppAvailabilityProvider().appAvailability = appAvailability
    }

    fun setIgnoringBatteryOptimizations(ignoringBatteryOptimizations: Boolean) {
        batteryOptimizationChecker.ignoringBatteryOptimizations = ignoringBatteryOptimizations
    }

    fun getIsolationPaymentTokenStateProvider() =
        component.getIsolationPaymentTokenStateProvider()

    fun getLastVisitedBookTestTypeVenueDateProvider() =
        component.getLastVisitedBookTestTypeVenueDateProvider()

    fun getAlarmManager() =
        component.getAlarmManager()

    companion object {
        const val ENGLISH_LOCAL_AUTHORITY = "E07000063"
    }
}

class MockClock(var currentInstant: Instant? = null) : Clock() {

    override fun instant(): Instant = currentInstant ?: Instant.now()

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = ZoneOffset.UTC

    fun reset() { currentInstant = null }
}

fun stringFromResId(@StringRes stringRes: Int): String {
    val resources = ApplicationProvider.getApplicationContext<ExposureApplication>().resources
    return resources.getString(stringRes)
}

class TestBluetoothStateProvider : AvailabilityStateProvider {
    val bluetoothStateMutable = SingleLiveEvent<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> =
        distinctUntilChanged(bluetoothStateMutable)

    override fun start(context: Context) {
        bluetoothStateMutable.postValue(bluetoothStateMutable.value)
    }

    override fun stop(context: Context) {
    }
}

class TestLocationStateProvider : AvailabilityStateProvider {
    val locationStateMutable = MutableLiveData<AvailabilityState>()
    override val availabilityState: LiveData<AvailabilityState> =
        distinctUntilChanged(locationStateMutable)

    override fun start(context: Context) {
        locationStateMutable.postValue(locationStateMutable.value)
    }

    override fun stop(context: Context) {
    }
}

class TestBatteryOptimizationChecker : BatteryOptimizationChecker {

    var ignoringBatteryOptimizations = false

    override fun isIgnoringBatteryOptimizations(): Boolean =
        ignoringBatteryOptimizations
}
