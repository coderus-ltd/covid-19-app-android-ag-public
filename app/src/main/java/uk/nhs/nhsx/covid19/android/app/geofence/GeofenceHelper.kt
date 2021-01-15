package uk.nhs.nhsx.covid19.android.app.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception

/**
 * This class is defined to assist in the setting up and applying of the
 * [GeofenceReceiver].
 *
 * Note: For maximum utility defining at risk areas will involve defining geofences here for each area and repeating
 * these steps for each one.
 *
 * To use you can simply:
 * 1. Create a [GeofenceHelper] instance and provide it with a [Context]
 *
 * 2. Define a [Geofence] specifying types 'GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT'
 * val geofence = geofenceHelper.getGeoFence(ID, latLong, radius, GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
 *
 *
 * 3. Create a [GeofencingRequest] with that [Geofence]
 * val geofencingRequest = geofenceHelper.getGeoFencingRequest(geofence)
 *
 * 4. Use an instance of [GeofencingClient] to register the [GeofencingRequest] with
 * the below [PendingIntent] to send updates Geofence updates to the [GeofenceReceiver]
 * geofencingClient.addGeofences(geofencingRequest, pendingIntent)
 */
class GeofenceHelper(private var base: Context?) : ContextWrapper(base) {

    /**
     * This should be used with [GeofencingClient] to register [GeofenceReceiver]
     * to receive updates on Geofence changes (via [GeofencingClient]).
     */
    private var pendingIntent: PendingIntent = run {
        val intent = Intent(base, GeofenceReceiver::class.java)
        PendingIntent.getBroadcast(
            base,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Takes a [Geofence] and creates a [GeofencingRequest] with it.
     * INITIAL_TRIGGER_ENTER indicates that geofencing service should trigger
     * Geofence.GEOFENCE_TRANSITION_ENTER notification at the moment when the
     * geofence is added and if the device is already inside that geofence.
     */
    fun getGeoFencingRequest(geofence: Geofence): GeofencingRequest = GeofencingRequest.Builder()
        .addGeofence(geofence)
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .build()

    /**
     * Creates a [Geofence] with the provided params.
     */
    fun getGeoFence(ID: String, latLng: LatLng, radius: Float, types: Int): Geofence = Geofence.Builder()
        .setRequestId(ID)
        .setCircularRegion(
            latLng.latitude, latLng.longitude, radius
        )
        .setTransitionTypes(types)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .build()

    /**
     * Returns the [GeofenceReceiver] PendingIntent.
     */
    fun getPendingIntent(): PendingIntent {
        return pendingIntent
    }

    /**
     * Geofence error handing.
     */
    fun getGeoFenceException(exception: Exception): String {
        if (exception is ApiException) return when(exception.statusCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many geofences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many Pending Intent"
            else -> "Default exception"
        }
        return exception.message.toString()
    }
}