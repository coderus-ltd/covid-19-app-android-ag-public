package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.SymptomsAndOnsetFlowConfiguration
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.report.notReported
import java.time.temporal.ChronoUnit

class ManualTestEntryAnalyticsTest : AnalyticsTest() {

    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)
    private val selfDiagnosis = SelfDiagnosis(this)

    @Test
    fun manuallyEnterPositivePCRTest_noSymptoms_thenGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            LAB_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = false,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositivePCRTest_confirmSymptoms_selectExplicitDate_thenGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            LAB_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = true,
                didRememberOnsetSymptomsDate = true
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositivePCRTest_confirmSymptoms_cannotRememberOnsetDate_thenGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            LAB_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = true,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveAssistedLFDTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_RESULT,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation,
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_SELF_REPORTED,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation,
            Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
            Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
            Metrics::hasTestedSelfRapidPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveUnconfirmedLFDTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_RESULT,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = true,
            expectedScreenState = PositiveWillBeInIsolationAndOrderTest,
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Manual
    // >0 if the app is aware that the user has received/entered a positive test
    // this currently happens during an isolation and for the 14 days after isolation
    private fun manuallyEnterPositiveTestAndGoIntoIsolation(
        testKitType: VirologyTestKitType,
        symptomsAndOnsetFlowConfiguration: SymptomsAndOnsetFlowConfiguration?,
        requiresConfirmatoryTest: Boolean,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters positive LFD test result on 2nd Jan
        // Isolation end date: 13th Jan
        manualTestResultEntry.enterPositive(
            testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            symptomsAndOnsetFlowConfiguration = symptomsAndOnsetFlowConfiguration,
            expectedScreenState = expectedScreenState
        )

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertEquals(1, Metrics::startedIsolation)
            if (symptomsAndOnsetFlowConfiguration != null) {
                assertEquals(1, Metrics::didAskForSymptomsOnPositiveTestEntry)
                if (symptomsAndOnsetFlowConfiguration.didHaveSymptoms) {
                    assertEquals(1, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                    assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                    assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                }
                if (symptomsAndOnsetFlowConfiguration.didRememberOnsetSymptomsDate) {
                    assertEquals(1, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                }
            }
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 4th-13th Jan -> Analytics packets for: 3rd-12th Jan
        assertOnFieldsForDateRange(4..13) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (symptomsAndOnsetFlowConfiguration?.didHaveSymptoms == true) {
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            }
        }

        // Dates: 14th-27th Jan -> Analytics packets for: 13th-26th Jan
        assertOnFieldsForDateRange(14..27) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            if (symptomsAndOnsetFlowConfiguration?.didHaveSymptoms == true) {
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            }
        }

        // Current date: 28th Jan -> Analytics packet for: 27th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterPositivePCRTestAfterSelfDiagnosisAndContinueIsolation() = notReported {
        manuallyEnterPositiveTestAfterSelfDiagnosisAndContinueIsolation(
            LAB_RESULT,
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveContinueIsolation,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Manual
    // >0 if the app is aware that the user has received/entered a positive test
    // this currently happens during an isolation and for the 14 days after isolation
    private fun manuallyEnterPositiveTestAfterSelfDiagnosisAndContinueIsolation(
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 11th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
        }

        // Enters positive LFD test result on 3rd Jan
        // Isolation end date: 11th Jan
        manualTestResultEntry.enterPositive(
            testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            expectedScreenState = expectedScreenState
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 5th-11th Jan -> Analytics packets for: 4th-10th Jan
        assertOnFieldsForDateRange(5..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24th Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 26th Jan -> Analytics packet for: 25th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterOldPositivePCRTestAfterSelfDiagnosisAndContinueIsolation() = notReported {
        manuallyEnterOldPositiveTestAfterSelfDiagnosisAndContinueIsolation(
            LAB_RESULT,
            requiresConfirmatoryTest = false,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Manual
    // >0 if the app is aware that the user has received/entered a positive test
    // this currently happens during an isolation and for the 14 days after isolation
    private fun manuallyEnterOldPositiveTestAfterSelfDiagnosisAndContinueIsolation(
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        val startDate = testAppContext.clock.instant()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 11th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
        }

        // Enters positive LFD test result on 3rd Jan
        // The old test deletes self-diagnosis and shortens the isolation
        // Isolation end date: 9th Jan
        val testEndDate = startDate.minus(2, ChronoUnit.DAYS)
        manualTestResultEntry.enterPositive(
            testKitType,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            testEndDate = testEndDate,
            expectedScreenState = PositiveContinueIsolation
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 5th-10th Jan -> Analytics packets for: 4th-9th Jan
        assertOnFieldsForDateRange(5..10) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 11th-24th Jan -> Analytics packets for: 10th-23rd Jan
        assertOnFieldsForDateRange(11..24) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(hasTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
        }

        // Current date: 25th Jan -> Analytics packet for: 24th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterNegativeTestResultSetsReceivedNegativeTestResultEnteredManually() = notReported {
        manualTestResultEntry.enterNegative()

        assertOnFields {
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, Metrics::receivedNegativeTestResultEnteredManually)
        }
    }

    @Test
    fun manuallyEnterVoidTestResultSetsReceivedVoidTestResultEnteredManually() = notReported {
        manualTestResultEntry.enterVoid()

        assertOnFields {
            assertEquals(1, Metrics::receivedVoidTestResult)
            assertEquals(1, Metrics::receivedVoidTestResultEnteredManually)
        }
    }
}
