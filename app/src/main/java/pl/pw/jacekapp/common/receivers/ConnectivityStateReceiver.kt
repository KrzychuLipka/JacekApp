package pl.pw.jacekapp.common.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log

class ConnectivityStateReceiver(
    private val context: Context,
    private val connectionReadyCallback: () -> Unit,
    private val connectionNotReadyCallback: () -> Unit,
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "pw.ConnStateReceiver"
    }

    private var gpsOn = false
    private var bluetoothOn = false

    init {
        initializeConnectionState()
    }

    private fun initializeConnectionState() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothOn = bluetoothAdapter?.isEnabled == true
        notifyConnectionChange()
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent == null) return
        when (val actionName = intent.action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> verifyGpsConnection()

            BluetoothAdapter.ACTION_STATE_CHANGED -> verifyBluetoothConnection(intent)

            else -> Log.d(TAG, "Intent action: $actionName")
        }
    }

    private fun verifyBluetoothConnection(
        intent: Intent,
    ) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        bluetoothOn = state == BluetoothAdapter.STATE_ON
        notifyConnectionChange()
    }

    private fun verifyGpsConnection() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        notifyConnectionChange()
    }

    private fun notifyConnectionChange() {
        if (gpsOn && bluetoothOn) {
            connectionReadyCallback()
        } else {
            connectionNotReadyCallback()
        }
    }
}
