package uk.nhs.nhsx.covid19.android.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.LocationPrefs

/**
 * Implementation of App Widget functionality.
 *
 * Used in combination with [FusedLocationService] and [LocationPrefs]
 *
 * Receives callbacks that trigger onUpdate() which then calls updateAppWidget to
 * update the TextViews.
 *
 * Example of use:
 *              // in your AndroidManifest.xml
 *              <receiver android:name=".widgets.NHSLocationWidget">
                    <intent-filter>
                        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
                    </intent-filter>

                    <meta-data
                        android:name="android.appwidget.provider"
                        android:resource="@xml/nhs_location_widget_info" />
                </receiver>
 */
class NHSLocationWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {

    /* The details to be displayed in the widget come from SharedPreferences
    * Specifically, postalCode and a description for that postalCode */
    val locationPref = LocationPrefs(context)
    val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java)
        .let { intent ->
            PendingIntent.getActivity(context, 0, intent, 0)
        }

    /* Construct the RemoteViews object */
    val views: RemoteViews = RemoteViews(context.packageName, R.layout.nhs_location_widget).apply {
        setOnClickPendingIntent(R.id.appwidget_image_location, pendingIntent)
    }

    views.setTextViewText(R.id.appwidget_text_title, locationPref.getPostalCode())
    views.setTextViewText(R.id.appwidget_text_desc, locationPref.getDescription())

    /* Instruct the widget manager to update the widget */
    appWidgetManager.updateAppWidget(appWidgetId, views)
}