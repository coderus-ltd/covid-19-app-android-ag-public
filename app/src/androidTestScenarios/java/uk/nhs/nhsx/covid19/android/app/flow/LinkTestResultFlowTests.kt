package uk.nhs.nhsx.covid19.android.app.flow

import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class LinkTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
    }

    @After
    fun tearDown() {
        testAppContext.clock.reset()
    }

    @Test
    fun startIndexCase_linkPositiveTestResult_shouldContinueIsolation() = notReported {
        testAppContext.setState(
            isolationHelper.selfAssessment()
                .copy(expiryDate = LocalDate.now().plus(7, ChronoUnit.DAYS))
                .asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation = 7)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startContactCase_linkJustExpiredPositiveTestResult_shouldEndIsolation() = notReported {
        val contactInstant = Instant.now(testAppContext.clock).minus(2, ChronoUnit.DAYS)
        testAppContext.virologyTestingApi.testEndDate = contactInstant.minus(10, ChronoUnit.DAYS)

        val contactDate = contactInstant.toLocalDate(testAppContext.clock.zone)
        testAppContext.setState(
            isolationHelper
                .contactCase(
                    exposureDate = contactDate,
                    notificationDate = contactDate,
                    expiryDate = contactDate.plusDays(10)
                )
                .asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveContactNoIndex()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation()
        }

        testResultRobot.clickGoodNewsActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertExpiredIndexNoContact()
    }

    @Test
    fun startContactCase_linkTooOldPositiveTestResult_shouldContinueIsolation() = notReported {
        val contactInstant = Instant.now(testAppContext.clock).minus(2, ChronoUnit.DAYS)
        testAppContext.virologyTestingApi.testEndDate = contactInstant.minus(11, ChronoUnit.DAYS)

        val contactDate = contactInstant.toLocalDate(testAppContext.clock.zone)
        testAppContext.setState(
            isolationHelper
                .contactCase(
                    exposureDate = contactDate,
                    notificationDate = contactDate,
                    expiryDate = contactDate.plusDays(10)
                )
                .asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveContactNoIndex()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation = 8)
        }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveContactNoIndex()
    }

    @Test
    fun startDefault_linkPositiveTestResult_noSymptoms_shouldIsolate() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(remainingDaysInIsolation = 9)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startDefault_linkPositiveTestResult_confirmSymptoms_selectSymptomsDate_shouldIsolate() = notReported {
        val now = Instant.parse("2021-01-10T10:00:00Z")
        testAppContext.clock.currentInstant = now
        testAppContext.virologyTestingApi.testEndDate = now.minus(2, ChronoUnit.DAYS)
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickYes()

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.clickSelectDate()

        val onsetDate = LocalDateTime.ofInstant(now, testAppContext.clock.zone)
            .toLocalDate()
            .minusDays(3)

        linkTestResultOnsetDateRobot.selectDayOfMonth(onsetDate.dayOfMonth)

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(remainingDaysInIsolation = 8)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }
}
