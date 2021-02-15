package uk.nhs.nhsx.covid19.android.app.payment

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.FAIL_SUCCEED_LOOP
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot

class RedirectToIsolationPaymentWebsiteActivityTest : EspressoTest() {

    private val isolationPaymentProgressRobot = ProgressRobot()

    @Before
    fun setUp() {
        testAppContext.setIsolationPaymentToken("abc")
        testAppContext.setState(
            State.Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = State.Isolation.ContactCase(
                    startDate = Instant.now().minus(3, ChronoUnit.DAYS),
                    notificationDate = Instant.now().minus(2, ChronoUnit.DAYS),
                    expiryDate = LocalDate.now().plus(1, ChronoUnit.DAYS)
                )
            )
        )
    }

    @Test
    fun opensBrowser() = notReported {
        assertBrowserIsOpened("about:blank") {
            startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() = notReported {
        MockApiModule.behaviour.responseType = FAIL_SUCCEED_LOOP

        startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()

        isolationPaymentProgressRobot.checkActivityIsDisplayed()

        isolationPaymentProgressRobot.checkErrorIsDisplayed()

        assertBrowserIsOpened("about:blank") {
            isolationPaymentProgressRobot.clickTryAgainButton()
        }
    }
}
