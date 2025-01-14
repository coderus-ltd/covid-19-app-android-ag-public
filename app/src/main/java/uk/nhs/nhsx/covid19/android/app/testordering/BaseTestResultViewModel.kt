package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

abstract class BaseTestResultViewModel : ViewModel() {

    protected val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    protected val navigationEventLiveData = SingleLiveEvent<NavigationEvent>()
    fun navigationEvent(): LiveData<NavigationEvent> = navigationEventLiveData

    abstract fun onCreate()

    abstract fun onActionButtonClicked()

    abstract fun onBackPressed()

    data class ViewState(
        val mainState: TestResultViewState,
        val remainingDaysInIsolation: Int
    )

    sealed class NavigationEvent {
        object NavigateToShareKeys : NavigationEvent()
        object NavigateToOrderTest : NavigationEvent()
        object Finish : NavigationEvent()
    }
}
