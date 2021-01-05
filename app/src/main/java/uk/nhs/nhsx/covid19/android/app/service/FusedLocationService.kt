package uk.nhs.nhsx.covid19.android.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.LocationPrefs
import uk.nhs.nhsx.covid19.android.app.widgets.NHSLocationWidget
import java.io.IOException
import java.util.Locale

/**
 * This service receives LocationCallback events when the device has been displaced from it's initial
 * location.
 *
 * This is required to be a Foreground Service to receive constant location updates.
 *
 * Example of usage:
 *      // in your MainActivity
 *      startForegroundService(Intent(this@MainActivity, FusedLocationService::class.java))
 *
 *
 * When the location is updated the service can then pass the post code to an external tool to get details
 * on the newly updated post code and display any important COVID-19 related information on the area they
 * are in alongside the updated postcode.
 *
 *
 * Required permissions in AndroidManifext.xml
 *
 *      <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
        <!--
            Required only when requesting background location access on
            Android 10 (API level 29) and higher.
        -->
        <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
 *
 *
 * Required service declaration in AndroidManifext.xml
 *
 * To be added within the <application></application> tags
 * <application>
 *     ...
 *     <service android:name=".service.FusedLocationService"/>
 * </application>
 */
class FusedLocationService: Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(IO + SupervisorJob())
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null
    private val geocoder = Geocoder(this, Locale.getDefault())

    var addresses: List<Address> = emptyList()

    /**
     * Returns null so that this Service cannot be bound.
     */
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Prepares the location callbacks when the Service starts.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand: ")
        startForeground(startId, startNotification())

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        job = scope.launch(IO) {
                locationRequest = LocationRequest().apply {
                    /* Defines how large a location displacement must be to trigger a callback (in meters, here 500f is 1000 meters) */
                    smallestDisplacement = 500f
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)
                        if (locationResult?.lastLocation != null) {
                            currentLocation = locationResult.lastLocation
                            locationResult.lastLocation.also { result ->
                                try {
                                    addresses = geocoder.getFromLocation(
                                        result.latitude,
                                        result.longitude,
                                        1
                                    )

                                    /*
                                    * When a location callback occurs record the new Post Code.
                                    *
                                    * Address format:
                                    * COUNTRY_CODE: ${addresses[0].countryCode}
                                    * COUNTRY_NAME: ${addresses[0].countryName}
                                    * POSTAL_CODE: ${addresses[0].postalCode}")
                                    * */
                                    val postalCode = addresses[0].postalCode.take(3)

                                    /* This desc message can be used to display any useful info to the user about what
                                    * their new area COVID status is.
                                    *
                                    * Here we would use another tool to retrieve a text description of
                                    * latest data for the new Postal Code.
                                    * */
                                    val desc = getString(R.string.widget_desc);

                                    /* 'postalCode' and 'desc' are stored in LocationPrefs */
                                    val locationPrefs = LocationPrefs(this@FusedLocationService)
                                    locationPrefs.apply {
                                        savePostalCode(postalCode)
                                        saveDescription(desc)
                                    }

                                    /* Tell the widget to update when the location changes */
                                    updateWidget()
                                } catch (io: IOException) {
                                    Timber.e(io, "onLocationResult: ${io.message}")
                                }
                            }

                        }
                    }
                }
                fusedLocationProviderClient.apply {
                    this?.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Creates the NotificationChannel and returns the Notification required
     * to be constantly displayed in the notification drawer for this foreground service.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun startNotification(): Notification {

        val channel = NotificationChannel(Companion.CHANNEL_ID,
                "Channel",
                NotificationManager.IMPORTANCE_DEFAULT)

        val notMan = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        notMan.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, Companion.CHANNEL_ID)
                .setContentTitle("NHS Covid-19 app")
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE).build()
        return notification
    }

    /**
     * Called to tell the widget to update.
     */
    fun updateWidget() {
        val man = AppWidgetManager.getInstance(this@FusedLocationService)
        val ids = man.getAppWidgetIds(ComponentName(this@FusedLocationService, NHSLocationWidget::class.java))
        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(updateIntent)
    }

    /**
     * Called when the Service is removed. Perform any clean up here.
     */
    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        /**
         * Notification channel ID.
         */
        private const val CHANNEL_ID = "nhs_widget_chan"
    }
}