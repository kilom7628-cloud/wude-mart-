package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class ResolvedDeliveryLocation(
  val latitude: Double,
  val longitude: Double,
  val city: String,
  val subCity: String,
  val addressLine: String,
)

class LocationHelper(private val context: Context) {

  fun hasLocationPermission(): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  suspend fun fetchOneTimeDeliveryAddress(): ResolvedDeliveryLocation? {
    if (!hasLocationPermission()) return null

    val location = withTimeoutOrNull(8_000L) { getSingleForegroundLocation() } ?: return null
    val geocoded = reverseGeocode(location.latitude, location.longitude)
    return ResolvedDeliveryLocation(
      latitude = location.latitude,
      longitude = location.longitude,
      city = geocoded?.locality?.ifBlank { null } ?: geocoded?.adminArea.orEmpty(),
      subCity = geocoded?.subLocality.orEmpty(),
      addressLine =
        buildString {
          val parts =
            listOfNotNull(
                geocoded?.thoroughfare?.takeIf { it.isNotBlank() },
                geocoded?.featureName?.takeIf { it.isNotBlank() && it != geocoded.thoroughfare },
                geocoded?.subLocality?.takeIf { it.isNotBlank() },
                geocoded?.locality?.takeIf { it.isNotBlank() },
              )
              .distinct()
          if (parts.isNotEmpty()) {
            append(parts.joinToString(", "))
          } else {
            append(String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude))
          }
        },
    )
  }

  @SuppressLint("MissingPermission")
  private suspend fun getSingleForegroundLocation(): Location? =
    suspendCancellableCoroutine { cont ->
      val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      if (manager == null) {
        cont.resume(null)
        return@suspendCancellableCoroutine
      }

      val providers =
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
          .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

      val bestLastKnown =
        providers
          .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
          .maxByOrNull { it.time }

      if (bestLastKnown != null && System.currentTimeMillis() - bestLastKnown.time < 5 * 60 * 1000L) {
        cont.resume(bestLastKnown)
        return@suspendCancellableCoroutine
      }

      val activeProvider = providers.firstOrNull()
      if (activeProvider == null) {
        cont.resume(bestLastKnown)
        return@suspendCancellableCoroutine
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        runCatching {
            manager.getCurrentLocation(
              activeProvider,
              signal,
              ContextCompat.getMainExecutor(context),
            ) { loc ->
              if (cont.isActive) cont.resume(loc ?: bestLastKnown)
            }
          }
          .onFailure {
            if (cont.isActive) cont.resume(bestLastKnown)
          }
      } else {
        @Suppress("DEPRECATION")
        val listener =
          object : LocationListener {
            override fun onLocationChanged(location: Location) {
              if (cont.isActive) cont.resume(location)
            }
            @Deprecated("Deprecated in Android SDK")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
          }
        cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
        runCatching {
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
          }
          .onFailure {
            if (cont.isActive) cont.resume(bestLastKnown)
          }
      }
    }

  private suspend fun reverseGeocode(lat: Double, lng: Double): Address? =
    withContext(Dispatchers.IO) {
      if (!Geocoder.isPresent()) return@withContext null
      val geocoder = Geocoder(context, Locale.getDefault())
      runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<Address?> { cont ->
              geocoder.getFromLocation(lat, lng, 1) { addresses ->
                if (cont.isActive) cont.resume(addresses.firstOrNull())
              }
            }
          } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
          }
        }
        .getOrNull()
    }
}
