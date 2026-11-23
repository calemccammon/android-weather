package com.calemccammon.androidweather.core.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.calemccammon.androidweather.core.domain.repository.LocationRepository
import com.calemccammon.androidweather.core.model.Place
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * The device's whereabouts from Google Play services, which picks among
 * Wi-Fi, cell towers and GPS itself.
 *
 * A forecast covers kilometres, so approximate location is all that is asked
 * for, here and in the manifest.
 */
internal class FusedLocationRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : LocationRepository {
        private val client = LocationServices.getFusedLocationProviderClient(context)

        @SuppressLint("MissingPermission") // Checked first thing.
        override suspend fun currentPlace(): Place? {
            val allowed =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            if (!allowed) return null

            // A fresh fix if one comes in time; failing that, wherever the device
            // last knew itself to be, which indoors is often all there is.
            val location = fresh() ?: client.lastLocation.awaitOrNull()
            return location?.let { Place.fromCoordinates(it.latitude, it.longitude) }
        }

        @SuppressLint("MissingPermission") // Checked by the one caller, above.
        private suspend fun fresh(): Location? {
            // Lets the request be dropped if the caller stops waiting.
            val cancellation = CancellationTokenSource()
            return client.getCurrentLocation(request, cancellation.token).awaitOrNull(cancellation::cancel)
        }

        /** The task's result; null if it has none or failed. */
        private suspend fun <T> Task<T>.awaitOrNull(onCancel: () -> Unit = {}): T? =
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { continuation.resume(it) }
                addOnFailureListener { continuation.resume(null) }
                continuation.invokeOnCancellation { onCancel() }
            }

        private companion object {
            val request: CurrentLocationRequest =
                CurrentLocationRequest
                    .Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    // A fix from the last few minutes will do: nobody outruns the weather.
                    .setMaxUpdateAgeMillis(Duration.ofMinutes(5).toMillis())
                    .setDurationMillis(Duration.ofSeconds(10).toMillis())
                    .build()
        }
    }
