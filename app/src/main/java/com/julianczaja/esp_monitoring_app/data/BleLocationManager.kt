package com.julianczaja.esp_monitoring_app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

class BleLocationManager(context: Context) {

    val isLocationForBleEnabled: Flow<Boolean> = if (Build.VERSION.SDK_INT >= 31) {
        flowOf(true)
    } else {
        callbackFlow {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val locationProviderChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    channel.trySend(isGpsEnabled || isNetworkEnabled)
                }
            }

            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            context.registerReceiver(locationProviderChangedReceiver, filter)

            channel.trySend(isEnabled(locationManager))

            awaitClose {
                context.unregisterReceiver(locationProviderChangedReceiver)
            }
        }
            .distinctUntilChanged()
            .conflate()
    }

    private fun isEnabled(locationManager: LocationManager) =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
