package uk.nhs.nhsx.covid19.android.app.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RedirectToIsolationPaymentWebsiteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val requestIsolationPaymentUrl = mockk<RequestIsolationPaymentUrl>(relaxed = true)
    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val loadPaymentUrlResultObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = RedirectToIsolationPaymentWebsiteViewModel(
        requestIsolationPaymentUrl,
        isolationPaymentTokenStateProvider,
        isolationStateMachine,
        analyticsEventProcessorMock,
        fixedClock
    )

    private val isolationStateContactCase = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = LocalDate.parse("2020-05-14"),
            notificationDate = LocalDate.parse("2020-05-15"),
            expiryDate = LocalDate.parse("2020-05-21")
        )
    )

    @Test
    fun `load isolation payment returns success`() = runBlocking {
        testSubject.fetchWebsiteUrl().observeForever(loadPaymentUrlResultObserver)

        every { isolationPaymentTokenStateProvider.tokenState } returns IsolationPaymentTokenState.Token("abc")
        coEvery {
            requestIsolationPaymentUrl.invoke(
                IsolationPaymentUrlRequest(
                    "abc",
                    Instant.parse("2020-05-14T00:00:00Z"),
                    Instant.parse("2020-05-21T00:00:00Z")
                )
            )
        } returns Success(
            IsolationPaymentUrlResponse(websiteUrlWithQuery = "https://website/abc")
        )
        every { isolationStateMachine.readLogicalState() } returns isolationStateContactCase.asLogical()

        testSubject.loadIsolationPaymentUrl()

        coVerify { analyticsEventProcessorMock.track(LaunchedIsolationPaymentsApplication) }
        verifyOrder {
            loadPaymentUrlResultObserver.onChanged(ViewState.Loading)
            loadPaymentUrlResultObserver.onChanged(
                ViewState.Success("https://website/abc")
            )
        }
    }

    @Test
    fun `load isolation payment returns failure`() = runBlocking {
        testSubject.fetchWebsiteUrl().observeForever(loadPaymentUrlResultObserver)

        val testException = Exception("Test error")

        every { isolationStateMachine.readLogicalState() } returns IsolationState(isolationConfiguration = DurationDays()).asLogical()
        coEvery { requestIsolationPaymentUrl.invoke(any()) } returns Failure(testException)

        testSubject.loadIsolationPaymentUrl()

        verifyOrder {
            loadPaymentUrlResultObserver.onChanged(ViewState.Loading)
            loadPaymentUrlResultObserver.onChanged(ViewState.Error)
        }
    }
}
