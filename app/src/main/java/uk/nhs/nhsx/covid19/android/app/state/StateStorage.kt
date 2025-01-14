package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateStorage @Inject constructor(
    private val stateStringStorage: StateStringStorage,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    moshi: Moshi
) {

    private val stateSerializationAdapter: JsonAdapter<IsolationStateJson> = moshi.adapter(IsolationStateJson::class.java)

    var state: IsolationState
        get() =
            stateStringStorage.prefsValue?.let {
                runCatching {
                    stateSerializationAdapter.fromJson(it)?.toState()
                }
                    .getOrElse {
                        Timber.e(it)
                        IsolationState(isolationConfigurationProvider.durationDays)
                    } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: IsolationState(isolationConfigurationProvider.durationDays)
        set(newState) {
            stateStringStorage.prefsValue =
                stateSerializationAdapter.toJson(newState.toStateJson())
        }
}

@JsonClass(generateAdapter = true)
data class IsolationStateJson(
    val configuration: DurationDays,
    val contact: ContactCase? = null,
    val testResult: AcknowledgedTestResult? = null,
    val symptomatic: SymptomaticCase? = null,
    val indexExpiryDate: LocalDate? = null, // TODO@splitIndexCase: remove
    val hasAcknowledgedEndOfIsolation: Boolean = false,
    val version: Int = 1
)

@JsonClass(generateAdapter = true)
data class SymptomaticCase(
    val selfDiagnosisDate: LocalDate,
    val onsetDate: LocalDate? = null
)

private fun IsolationState.toStateJson(): IsolationStateJson =
    IsolationStateJson(
        configuration = isolationConfiguration,
        contact = contactCase,
        testResult = indexInfo?.testResult,
        symptomatic = indexInfo?.toSymptomaticCase(),
        indexExpiryDate = (indexInfo as? IndexCase)?.expiryDate,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

private fun IndexInfo.toSymptomaticCase(): SymptomaticCase? =
    if (this is IndexCase && isolationTrigger is SelfAssessment)
        SymptomaticCase(
            selfDiagnosisDate = isolationTrigger.selfAssessmentDate,
            onsetDate = isolationTrigger.onsetDate
        )
    else null

private fun IsolationStateJson.toState(): IsolationState {
    val indexIsolationTrigger =
        if (symptomatic != null)
            SelfAssessment(
                selfAssessmentDate = symptomatic.selfDiagnosisDate,
                onsetDate = symptomatic.onsetDate
            )
        else if (testResult != null && testResult.testResult == POSITIVE)
            PositiveTestResult(testEndDate = testResult.testEndDate)
        else null

    val indexInfo =
        if (indexIsolationTrigger != null && indexExpiryDate != null /*TODO@splitIndexCase: remove indexExpiryDate != null*/)
            IndexCase(
                isolationTrigger = indexIsolationTrigger,
                testResult = testResult,
                expiryDate = indexExpiryDate
            )
        else if (testResult?.testResult == NEGATIVE)
            NegativeTest(testResult)
        else null

    return IsolationState(
        isolationConfiguration = configuration,
        indexInfo = indexInfo,
        contactCase = contact,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )
}

@Singleton
class StateStringStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    companion object {
        private const val VALUE_KEY = "ISOLATION_STATE_KEY"
    }

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var prefsValue: String? by prefs
}
