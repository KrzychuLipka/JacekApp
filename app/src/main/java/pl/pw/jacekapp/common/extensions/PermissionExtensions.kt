package pl.pw.jacekapp.common.extensions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pl.pw.jacekapp.R

val scanBeaconsPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
} else {
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
}

fun AppCompatActivity.getMultiplePermissionsLauncher(
    allPermissionsGrantedCallback: () -> Unit,
) = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    if (permissions.entries.all { it.value }) {
        allPermissionsGrantedCallback()
    } else {
        Toast.makeText(
            this,
            getString(R.string.required_permissions_info),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun AppCompatActivity.allPermissionsGranted(
    permissions: Array<String>,
): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}
