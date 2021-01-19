package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_test_result.goodNewsContainer
import kotlinx.android.synthetic.main.activity_test_result.isolationRequestContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsActionButton
import kotlinx.android.synthetic.main.view_good_news.goodNewsIcon
import kotlinx.android.synthetic.main.view_good_news.goodNewsInfoView
import kotlinx.android.synthetic.main.view_good_news.goodNewsParagraphContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsSubtitle
import kotlinx.android.synthetic.main.view_good_news.goodNewsTitle
import kotlinx.android.synthetic.main.view_isolation_request.exposureFaqsLink
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestActionButton
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestImage
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestInfoView
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestParagraphContainer
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle1
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle2
import kotlinx.android.synthetic.main.view_toolbar_background.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.inPortraitMode
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class TestResultActivity : BaseActivity(R.layout.activity_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<TestResultViewModel>

    private val viewModel: TestResultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startViewModelListeners()

        viewModel.onCreate()
    }

    private fun startViewModelListeners() {
        viewModel.navigateToShareKeys().observe(this) { testResult ->
            ShareKeysInformationActivity.start(this, testResult)
        }

        viewModel.viewState().observe(
            this,
            Observer { viewState ->
                when (viewState.mainState) {
                    NegativeNotInIsolation ->
                        showAreNotIsolatingScreenOnNegative()
                    NegativeWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnNegative(viewState.remainingDaysInIsolation)
                    NegativeWontBeInIsolation ->
                        showDoNotHaveToSelfIsolateScreenOnNegative()
                    is PositiveContinueIsolation ->
                        showContinueToSelfIsolationScreenOnPositive(viewState.remainingDaysInIsolation)
                    is PositiveWillBeInIsolation ->
                        showSelfIsolateScreenOnPositive(viewState.remainingDaysInIsolation)
                    is PositiveWontBeInIsolation ->
                        showDoNotHaveToSelfIsolateScreenOnPositive()
                    PositiveThenNegativeWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnPositiveThenNegative(viewState.remainingDaysInIsolation)
                    VoidNotInIsolation ->
                        showAreNotIsolatingScreenOnVoid()
                    VoidWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnVoid(viewState.remainingDaysInIsolation)
                    Ignore -> finish()
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_ORDER_A_TEST) {
            navigateToStatusActivity()
        } else {
            finish()
        }
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    private fun showContinueToSelfIsolationScreenOnPositive(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_continue_self_isolate_explanation_1),
            getString(R.string.test_result_positive_continue_self_isolate_explanation_2),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonForPositiveTestResultClicked()
        }
    }

    private fun showContinueToSelfIsolationScreenOnNegative(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )

        isolationRequestInfoView.stateText = getString(R.string.state_test_negative_info)
        isolationRequestInfoView.stateColor = getColor(R.color.amber)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_continue_self_isolate_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.back_to_home)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            finish()
        }
    }

    private fun showContinueToSelfIsolationScreenOnVoid(remainingDaysInIsolation: Int) {
        setCloseToolbar(
            toolbar,
            R.string.empty,
            R.drawable.ic_close_primary
        ) {
            viewModel.acknowledgeTestResult()
        }

        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_book_test)

        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_void_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_void_continue_self_isolate_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.book_free_test)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            startActivityForResult(
                TestOrderingActivity.getIntent(this),
                REQUEST_CODE_ORDER_A_TEST
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.acknowledgeTestResult()
    }

    private fun showDoNotHaveToSelfIsolateScreenOnPositive() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.text = getString(R.string.test_result_your_test_result)
        title = goodNewsTitle.text
        goodNewsTitle.visible()

        goodNewsSubtitle.text = getString(R.string.test_result_positive_no_self_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonForPositiveTestResultClicked()
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnNegative() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_negative_or_finished)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.visible()
        title = goodNewsTitle.text

        goodNewsSubtitle.text =
            getString(R.string.test_result_negative_no_self_isolation_subtitle_text)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(
            getString(R.string.for_further_advice_visit)
        )

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            finish()
        }
    }

    private fun showAreNotIsolatingScreenOnNegative() {
        goodNewsContainer.visible()

        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.visible()
        title = goodNewsTitle.text

        goodNewsSubtitle.text =
            getString(R.string.test_result_negative_already_not_in_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            finish()
        }
    }

    private fun setSelfIsolateTitles(
        title1: String,
        title2: String
    ) {
        isolationRequestTitle1.text = title1
        isolationRequestTitle2.text = title2
        title = "$title1 $title2"
    }

    private fun showContinueToSelfIsolationScreenOnPositiveThenNegative(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()

        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )

        isolationRequestInfoView.stateText =
            getString(R.string.state_test_positive_then_negative_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_then_negative_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            finish()
        }
    }

    private fun showSelfIsolateScreenOnPositive(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.self_isolate_for),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_then_positive_continue_explanation),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonForPositiveTestResultClicked()
        }
    }

    private fun showAreNotIsolatingScreenOnVoid() {
        setCloseToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary) {
            viewModel.acknowledgeTestResult()
        }

        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.text = getString(R.string.test_result_your_test_result)
        title = getString(R.string.test_result_your_test_result)
        goodNewsTitle.visible()

        goodNewsSubtitle.text =
            getString(R.string.test_result_void_already_not_in_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.book_free_test)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.acknowledgeTestResult()
            startActivityForResult(
                TestOrderingActivity.getIntent(this),
                REQUEST_CODE_ORDER_A_TEST
            )
        }
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1339
    }
}
