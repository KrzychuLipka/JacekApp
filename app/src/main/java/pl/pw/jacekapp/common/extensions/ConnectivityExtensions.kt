package pl.pw.jacekapp.common.extensions

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.LocationManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import pl.pw.jacekapp.common.receivers.ConnectivityStateReceiver

private const val TAG = "pw.ConnectivityExtensions"
private var connectivityStateReceiver: BroadcastReceiver? = null
private var isReceiverRegistered = false

fun AppCompatActivity.listenForConnectionChanges(
    connectionReadyCallback: () -> Unit,
) {
    val intentFilter = IntentFilter().apply {
        addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
    unregisterConnectivityStateReceiverIfNeeded()
    connectivityStateReceiver = ConnectivityStateReceiver(
        context = this,
        connectionReadyCallback = connectionReadyCallback,
        connectionNotReadyCallback = {
            Log.d(TAG, "The connection has not been established yet.")
        },
    )
    registerReceiver(connectivityStateReceiver, intentFilter)
    isReceiverRegistered = true
}

fun AppCompatActivity.unregisterConnectivityStateReceiverIfNeeded() {
    if (!isReceiverRegistered) return
    connectivityStateReceiver?.let {
        try {
            unregisterReceiver(it)
            isReceiverRegistered = false
        } catch (exception: Exception) {
            Log.e(TAG, exception.localizedMessage, exception)
        }
    }
}
