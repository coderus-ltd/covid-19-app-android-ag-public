package uk.nhs.nhsx.covid19.android.app.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider

const val CHANNEL_NAME = "NHS-COVID-19"
const val CHANNEL_DESCRIPTION = "AREA CHANGED: Inform you that your risk level of Covid-19 has changed"

/**
 * Listens out for geofence transition callbacks.
 *
 * Captures Geofence.GEOFENCE_TRANSITION_ENTER and Geofence.GEOFENCE_TRANSITION_EXIT events
 * and creates Notifications to notify the user if they have entered/ exited identified
 * at risk areas.
 *
 * These areas should be established as Geofences using the [GeofenceHelper].
 *
 * NOTE: This requires the addition of android.permission.ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION and ACCESS_BACKGROUND_LOCATION
 * so the user is required to allow those permissions before this will work.
 */
class GeofenceReceiver : BroadcastReceiver() {

    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationManager: NotificationManager
    private lateinit var geofencingEvent: GeofencingEvent
    private val notificationID = 0

    /**
     * Called when a new EXIT/ENTER geofence event has been captured.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        geofencingEvent = GeofencingEvent.fromIntent(intent)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationChannel = NotificationChannel(NotificationProvider.RISK_CHANGED_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            notificationManager.createNotificationChannel(this)
        }

        if (geofencingEvent.hasError()) {
            Timber.d("onReceive: GEO_FENCING ERROR: ${geofencingEvent.errorCode}")
            return
        }
        when(geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->  {
                createNotification(context, "ENTERED DANGEROUS ZONE", CHANNEL_DESCRIPTION)
                Toast.makeText(context, "ENTERED DANGEROUS ZONE", Toast.LENGTH_LONG).show()
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                createNotification(context, "EXITED DANGEROUS ZONE", CHANNEL_DESCRIPTION)
                Toast.makeText(context, "EXITED DANGEROUS ZONE", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Create and show the warning notification.
     */
    private fun createNotification(context: Context, message: String, title: String) {
        val notification = NotificationCompat.Builder(context, NotificationProvider.RISK_CHANGED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nhs_covid_logo)
            .setColor(ContextCompat.getColor(context, R.color.nhs_blue))
            .setLargeIcon(generateBitmapFromVectorDrawable(context, R.drawable.ic_nhs_covid_logo))
            .setStyle(NotificationCompat.BigTextStyle().setSummaryText("").setBigContentTitle(title).bigText(message))
            .setColorized(true)
            .setContentTitle(title)
            .setContentText(message)
        with(NotificationManagerCompat.from(context)) {
            notify(notificationID, notification.build())
        }
    }

    /**
     * <Optional> Large Icon for .setLargeIcon() in createNotification().
     */
    private fun generateBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as Drawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}