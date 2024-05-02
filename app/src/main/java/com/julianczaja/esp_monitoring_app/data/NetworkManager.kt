package com.julianczaja.esp_monitoring_app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkManager(context: Context) {

    val isOnline: Flow<Boolean> = callbackFlow {

        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                channel.trySend(false)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        channel.trySend(connectivityManager.isCurrentlyConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
        .distinctUntilChanged()
        .conflate()

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean = activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        ?: false
}
