package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPrefsDelegate<T> @Inject constructor(
    private val sharedPref: SharedPreferences,
    private val valueKey: String,
    private val commit: Boolean
) : ReadWriteProperty<Any?, T?> {

    companion object {
        const val fileName = "encryptedSharedPreferences"
        const val migrationSharedPreferencesFileName = "migrationEncryptedSharedPreferences"

        fun <T> SharedPreferences.with(
            valueKey: String,
            commit: Boolean = false
        ): SharedPrefsDelegate<T> =
            SharedPrefsDelegate(this, valueKey, commit)
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T? =
        sharedPref.all[valueKey] as? T

    override operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?
    ) {
        sharedPref.edit(commit) {
            when (value) {
                is String -> putString(valueKey, value)
                is Float -> putFloat(valueKey, value)
                is Long -> putLong(valueKey, value)
                is Boolean -> putBoolean(valueKey, value)
                is Int -> putInt(valueKey, value)
                null -> remove(valueKey)
            }
        }
    }
}
