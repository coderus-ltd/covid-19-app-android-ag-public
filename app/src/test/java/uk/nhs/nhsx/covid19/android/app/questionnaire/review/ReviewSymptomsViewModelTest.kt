package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomAdvice.DoNotIsolate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomAdvice.Isolate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom1
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom2
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom3
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom4
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.NegativeHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.PositiveHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewSymptomsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val riskCalculator = mockk<RiskCalculator>()
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val navigateToIsolationScreenObserver = mockk<Observer<SymptomAdvice>>(relaxed = true)
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T20:00:00Z"), ZoneOffset.UTC)
    private val testSubject =
        ReviewSymptomsViewModel(isolationStateMachine, riskCalculator, analyticsManager, fixedClock)

    @Before
    fun setUp() {
        every { isolationStateMachine.processEvent(any()) } returns mockk()
        every { isolationStateMachine.readLogicalState() } returns
            IsolationState(isolationConfiguration = DurationDays()).asLogical()
    }

    @Test
    fun `setup outputs correct viewState`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.setup(questions, 100.0f, symptomsOnsetWindowDays)

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = properReviewSymptomItems,
                    onsetDate = NotStated,
                    showOnsetDateError = false,
                    symptomsOnsetWindowDays = symptomsOnsetWindowDays
                )
            )
        }
    }

    @Test
    fun `onDateSelected sets onsetDate`() {
        testSubject.viewState().observeForever(viewStateObserver)

        val date = Instant.parse("2020-05-21T10:00:00Z")

        testSubject.onDateSelected(dateInMillis = date.toEpochMilli())

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = listOf(),
                    onsetDate = ExplicitDate(LocalDate.parse("2020-05-21")),
                    showOnsetDateError = false,
                    symptomsOnsetWindowDays = 0
                )
            )
        }
    }

    @Test
    fun `cannotRememberDateChecked sets onsetDate`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateChecked()

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = listOf(),
                    onsetDate = CannotRememberDate,
                    showOnsetDateError = false,
                    symptomsOnsetWindowDays = 0
                )
            )
        }
    }

    @Test
    fun `cannotRememberDateUnchecked if date is explicitly stated`() {
        testSubject.viewState.value = testSubject.viewState.value?.copy(
            onsetDate = ExplicitDate(
                LocalDate.parse("2020-05-21")
            ),
            showOnsetDateError = true
        )
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = listOf(),
                    onsetDate = ExplicitDate(LocalDate.parse("2020-05-21")),
                    showOnsetDateError = false,
                    symptomsOnsetWindowDays = 0
                )
            )
        }
    }

    @Test
    fun `cannotRememberDateUnchecked if no date is explicitly stated`() {
        testSubject.viewState.value = testSubject.viewState.value?.copy(
            onsetDate = CannotRememberDate,
            showOnsetDateError = true
        )
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = listOf(),
                    onsetDate = NotStated,
                    showOnsetDateError = false,
                    symptomsOnsetWindowDays = 0
                )
            )
        }
    }

    @Test
    fun `onButtonConfirmedClicked but date is not stated`() {
        testSubject.viewState.value = testSubject.viewState.value?.copy(
            onsetDate = NotStated
        )
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onButtonConfirmedClicked()

        verify {
            viewStateObserver.onChanged(
                ViewState(
                    reviewSymptomItems = listOf(),
                    onsetDate = NotStated,
                    showOnsetDateError = true,
                    symptomsOnsetWindowDays = 0
                )
            )
        }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is selected does not update view state`() {
        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        setupStateWithSelectedOnsetDate(onsetDate)
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns true

        testSubject.onButtonConfirmedClicked()

        verify(exactly = 0) {
            viewStateObserver.onChanged(any())
        }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is stated calls riskCalculator`() {
        setupStateWithSelectedOnsetDate()
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns true

        testSubject.onButtonConfirmedClicked()

        verify { riskCalculator.isRiskAboveThreshold(listOf(symptom1, symptom3), riskThreshold) }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is stated and user has coronavirus symptoms updates isolation state machine`() {
        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        setupStateWithSelectedOnsetDate(onsetDate)
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns true

        testSubject.onButtonConfirmedClicked()

        verify {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
        }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is stated and user has no coronavirus symptoms does not update isolation state machine`() {
        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        setupStateWithSelectedOnsetDate(onsetDate)
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns false

        testSubject.onButtonConfirmedClicked()

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }
    }

    @Test
    fun `onButtonConfirmedClicked and user should not self-isolate`() {
        testSubject.navigateToSymptomAdviceScreen().observeForever(navigateToIsolationScreenObserver)
        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        setupStateWithSelectedOnsetDate(onsetDate)
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns false

        testSubject.onButtonConfirmedClicked()

        verify { navigateToIsolationScreenObserver.onChanged(DoNotIsolate) }
    }

    @Test
    fun `onButtonConfirmedClicked and user should self-isolate`() {
        val remainingDaysInIsolation = 2
        every { isolationStateMachine.remainingDaysInIsolation(any()) } returns remainingDaysInIsolation.toLong()
        testSubject.navigateToSymptomAdviceScreen().observeForever(navigateToIsolationScreenObserver)
        val onsetDate = LocalDate.parse("2020-05-21")
        setupStateWithSelectedOnsetDate(ExplicitDate(onsetDate))
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns true
        every { isolationStateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = IndexCase(
                    isolationTrigger = SelfAssessment(
                        selfAssessmentDate = LocalDate.now(fixedClock),
                        onsetDate
                    ),
                    expiryDate = LocalDate.parse("2020-05-24")
                )
            ).asLogical()

        testSubject.onButtonConfirmedClicked()

        verify { navigateToIsolationScreenObserver.onChanged(Isolate(true, remainingDaysInIsolation)) }
    }

    @Test
    fun `onButtonConfirmedClicked and user should self-isolate although negative symptoms when contact case`() {
        val remainingDaysInIsolation = 2
        every { isolationStateMachine.remainingDaysInIsolation(any()) } returns remainingDaysInIsolation.toLong()
        testSubject.navigateToSymptomAdviceScreen().observeForever(navigateToIsolationScreenObserver)
        val onsetDate = LocalDate.parse("2020-05-21")
        setupStateWithSelectedOnsetDate(ExplicitDate(onsetDate))
        every { riskCalculator.isRiskAboveThreshold(any(), any()) } returns false
        every { isolationStateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    exposureDate = LocalDate.parse("2020-05-19"),
                    notificationDate = LocalDate.parse("2020-05-19"),
                    expiryDate = LocalDate.parse("2020-05-24")
                )
            ).asLogical()

        testSubject.onButtonConfirmedClicked()

        verify { navigateToIsolationScreenObserver.onChanged(Isolate(false, remainingDaysInIsolation)) }
    }

    @Test
    fun `onset date in future is invalid`() {
        assertFalse(testSubject.isOnsetDateValid(fixedClock.instant().plusSeconds(10).toEpochMilli(), 14))
    }

    @Test
    fun `onset date outside symptoms onset window is invalid`() {
        assertFalse(testSubject.isOnsetDateValid(fixedClock.instant().minus(14, ChronoUnit.DAYS).toEpochMilli(), 14))
    }

    @Test
    fun `onset date in the past and within symptoms onset window is valid`() {
        assertTrue(testSubject.isOnsetDateValid(fixedClock.instant().minus(13, ChronoUnit.DAYS).toEpochMilli(), 14))
    }

    private fun setupStateWithSelectedOnsetDate(onsetDate: SelectedDate = CannotRememberDate) {
        val state = testSubject.viewState.value?.copy(
            reviewSymptomItems = properReviewSymptomItems,
            onsetDate = onsetDate
        )
        testSubject.viewState.value = state
        testSubject.riskThreshold = riskThreshold
    }

    companion object {
        private val question1 = Question(symptom = symptom1, isChecked = true)
        private val question2 = Question(symptom = symptom2, isChecked = false)
        private val question3 = Question(symptom = symptom3, isChecked = true)
        private val question4 = Question(symptom = symptom4, isChecked = false)
        val questions = listOf(
            question1,
            question2,
            question3,
            question4
        )
        const val symptomsOnsetWindowDays = 14
        const val riskThreshold = 100F
        val properReviewSymptomItems = listOf(
            PositiveHeader,
            Question(symptom1, true),
            Question(symptom3, true),
            NegativeHeader,
            Question(symptom2, false),
            Question(symptom4, false)
        )
    }
}
