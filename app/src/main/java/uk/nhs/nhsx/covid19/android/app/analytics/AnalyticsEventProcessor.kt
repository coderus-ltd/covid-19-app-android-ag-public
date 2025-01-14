package uk.nhs.nhsx.covid19.android.app.analytics

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DECLARED_NEGATIVE_RESULT_FROM_DCT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M1_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M2_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.ISOLATION_STATE_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEventProcessor @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val notificationProvider: NotificationProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val clock: Clock
) {

    suspend fun track(analyticsEvent: AnalyticsEvent) {
        val isOnboardingCompleted = onboardingCompletedProvider.value.defaultFalse()
        if (!isOnboardingCompleted) {
            return
        }

        Timber.d("processing event: $analyticsEvent")

        val logItem = analyticsEvent.toAnalyticsLogItem()

        analyticsLogStorage.add(AnalyticsLogEntry(Instant.now(clock), logItem))
    }

    private suspend fun AnalyticsEvent.toAnalyticsLogItem() = when (this) {
        AcknowledgedStartOfIsolationDueToRiskyContact -> Event(ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT)
        QrCodeCheckIn -> Event(QR_CODE_CHECK_IN)
        CanceledCheckIn -> Event(CANCELED_CHECK_IN)
        CompletedQuestionnaireAndStartedIsolation -> Event(
            COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
        )
        CompletedQuestionnaireButDidNotStartIsolation -> Event(
            COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
        )
        BackgroundTaskCompletion -> AnalyticsLogItem.BackgroundTaskCompletion(getBackgroundTaskTicks())
        PositiveResultReceived -> Event(POSITIVE_RESULT_RECEIVED)
        NegativeResultReceived -> Event(NEGATIVE_RESULT_RECEIVED)
        VoidResultReceived -> Event(VOID_RESULT_RECEIVED)
        ReceivedRiskyContactNotification -> Event(RECEIVED_RISKY_CONTACT_NOTIFICATION)
        RiskyContactReminderNotification -> Event(RISKY_CONTACT_REMINDER_NOTIFICATION)
        StartedIsolation -> Event(STARTED_ISOLATION)
        is ResultReceived -> AnalyticsLogItem.ResultReceived(result, testKitType, testOrderType)
        UpdateNetworkStats -> updateNetworkStats()
        ReceivedActiveIpcToken -> Event(RECEIVED_ACTIVE_IPC_TOKEN)
        SelectedIsolationPaymentsButton -> Event(SELECTED_ISOLATION_PAYMENTS_BUTTON)
        LaunchedIsolationPaymentsApplication -> Event(LAUNCHED_ISOLATION_PAYMENTS_APPLICATION)
        LaunchedTestOrdering -> Event(LAUNCHED_TEST_ORDERING)
        is ExposureWindowsMatched -> ExposureWindowMatched(totalRiskyExposures, totalNonRiskyExposures)
        ReceivedUnconfirmedPositiveTestResult -> Event(RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT)
        DeclaredNegativeResultFromDct -> Event(DECLARED_NEGATIVE_RESULT_FROM_DCT)
        DidHaveSymptomsBeforeReceivedTestResult -> Event(DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT)
        DidRememberOnsetSymptomsDateBeforeReceivedTestResult ->
            Event(DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT)
        DidAskForSymptomsOnPositiveTestEntry -> Event(DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY)
        ReceivedRiskyVenueM1Warning -> Event(RECEIVED_RISKY_VENUE_M1_WARNING)
        ReceivedRiskyVenueM2Warning -> Event(RECEIVED_RISKY_VENUE_M2_WARNING)
        TotalAlarmManagerBackgroundTasks -> Event(TOTAL_ALARM_MANAGER_BACKGROUND_TASKS)
    }

    private fun updateNetworkStats() = AnalyticsLogItem.UpdateNetworkStats(
        downloadedBytes = networkTrafficStats.getTotalBytesDownloaded(),
        uploadedBytes = networkTrafficStats.getTotalBytesUploaded()
    )

    private suspend fun getBackgroundTaskTicks() =
        BackgroundTaskTicks().apply {
            if (!appAvailabilityProvider.isAppAvailable()) {
                return this
            }

            runningNormallyBackgroundTick = exposureNotificationApi.isRunningNormally()

            val currentState = IsolationLogicalState.from(stateStorage.state)

            if (currentState is PossiblyIsolating && currentState.isActiveIsolation(clock)) {
                isIsolatingBackgroundTick = true
                isIsolatingForHadRiskyContactBackgroundTick = currentState.isActiveContactCase(clock)
                isIsolatingForSelfDiagnosedBackgroundTick = currentState.getActiveIndexCase(clock)?.isSelfAssessment() ?: false

                currentState.getTestResultIfPositive()?.let { acknowledgedPositiveTestResult ->
                    val isolationStartDate = currentState.startDate
                    val testResultAcknowledgeDate = acknowledgedPositiveTestResult.acknowledgedDate

                    if (testResultAcknowledgeDate.isEqualOrAfter(isolationStartDate)) {
                        val testKitType = acknowledgedPositiveTestResult.testKitType
                        isIsolatingForTestedPositiveBackgroundTick = testKitType == LAB_RESULT || testKitType == null
                        isIsolatingForTestedLFDPositiveBackgroundTick = testKitType == RAPID_RESULT
                        isIsolatingForTestedSelfRapidPositiveBackgroundTick = testKitType == RAPID_SELF_REPORTED
                        isIsolatingForUnconfirmedTestBackgroundTick =
                            !acknowledgedPositiveTestResult.isConfirmed()
                    }
                }
            }

            if (currentState is PossiblyIsolating) {
                hasHadRiskyContactBackgroundTick = currentState.remembersContactCase()
                hasSelfDiagnosedPositiveBackgroundTick = currentState.remembersIndexCase()
                hasSelfDiagnosedBackgroundTick = currentState.remembersIndexCaseWithSelfAssessment()
                currentState.getTestResultIfPositive()?.let { acknowledgedPositiveTestResult ->
                    val testKitType = acknowledgedPositiveTestResult.testKitType
                    hasTestedPositiveBackgroundTick = testKitType == LAB_RESULT || testKitType == null
                    hasTestedLFDPositiveBackgroundTick = testKitType == RAPID_RESULT
                    hasTestedSelfRapidPositiveBackgroundTick = testKitType == RAPID_SELF_REPORTED
                }
            }

            haveActiveIpcTokenBackgroundTick =
                isolationPaymentTokenStateProvider.tokenState is IsolationPaymentTokenState.Token

            encounterDetectionPausedBackgroundTick = !exposureNotificationApi.isEnabled()

            hasRiskyContactNotificationsEnabledBackgroundTick =
                notificationProvider.isChannelEnabled(ISOLATION_STATE_CHANNEL_ID)

            hasReceivedRiskyVenueM2WarningBackgroundTick =
                lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()
        }
}
