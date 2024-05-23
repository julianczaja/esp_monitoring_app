package com.julianczaja.esp_monitoring_app.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class BluetoothManager(context: Context) {

    val isBluetoothEnabled: Flow<Boolean> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> channel.trySend(true)
                    BluetoothAdapter.STATE_OFF -> channel.trySend(false)
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(bluetoothReceiver, filter)
        }

        channel.trySend(bluetoothAdapter.isEnabled)

        awaitClose {
            context.unregisterReceiver(bluetoothReceiver)
        }
    }
        .distinctUntilChanged()
        .conflate()
}
