package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import uk.nhs.nhsx.covid19.android.app.widgets.NHSLocationWidget

/**
 * Convenience class for storing data in the users SharedPreferences.
 *
 * Postal Code and Description are recorded in SharedPreferences and recalled to
 * update the [NHSLocationWidget].
 */
class LocationPrefs(private val context: Context) {

    /**
     * The SharedPreference KEYS to store the relevant data.
     */
    companion object {
        private const val PREFS_NAME = "uk.nhs.covid.app"
        private const val POSTAL_KEY = "postal_code"
        private const val DESC_KEY = "description"
    }

    /**
     * Store new Postal Code in SharedPreferences.
     */
    internal fun savePostalCode(value: String) {
        val postalCode = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        postalCode.putString(POSTAL_KEY, value).apply()
    }
    /**
     * Store new Description in SharedPreferences.
     */
    internal fun saveDescription(value: String) {
        val description = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        description.putString(DESC_KEY, value).apply()
    }

    /**
     * Retrieve stored Postal Code from SharedPreferences.
     */
    internal fun getPostalCode(): String? =
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(POSTAL_KEY, "")
    /**
     * Retrieve stored Description from SharedPreferences.
     */
    internal fun getDescription(): String? =
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(DESC_KEY, "")
}