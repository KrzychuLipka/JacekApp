package pl.pw.jacekapp.common.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.altbeacon.beacon.Beacon
import pl.pw.jacekapp.R
import pl.pw.jacekapp.data.model.FingerprintingLogColumns
import pl.pw.jacekapp.data.model.PositioningLogColumns
import pl.pw.jacekapp.data.model.PositioningLogEntry
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

class Logger @Inject constructor(
    private val context: Context,
) {

    companion object {
        private const val TAG = "pw.Logger"
        private const val FINGERPRINTING_LOGS_FILE_NAME = "fingerprinting.tsv"
        private const val POSITIONING_LOGS_FILE_NAME = "positioning.tsv"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val positioningLogsFile: File by lazy {
        File(context.filesDir.absolutePath + "/$POSITIONING_LOGS_FILE_NAME")
    }
    private val fingerprintingLogsFile: File by lazy {
        File(context.filesDir.absolutePath + "/$FINGERPRINTING_LOGS_FILE_NAME")
    }
    private val formattedPositioningColumns =
        "${PositioningLogColumns.QR_SCAN_TIMESTAMP.value}\t" +
                "${PositioningLogColumns.QR_POSITION.value}\t" +
                "${PositioningLogColumns.TAG_TIMESTAMP.value}\t" +
                "${PositioningLogColumns.TAG.value}\n"
    private val radioMap = mutableMapOf<Long, Map<String, Int>>()

    init {
        if (positioningLogsFile.exists()) {
            positioningLogsFile.delete()
        }
        if (fingerprintingLogsFile.exists()) {
            fingerprintingLogsFile.delete()
        }
    }

    fun appendPositioningLog(
        positioningLogEntry: PositioningLogEntry,
    ) {
        val formattedValues = "${positioningLogEntry.qrScanTimestamp ?: ""}\t" +
                "${positioningLogEntry.qrPosition ?: ""}\t" +
                "${positioningLogEntry.tagTimestamp ?: ""}\t" +
                "${positioningLogEntry.tagType ?: ""}\n"
        coroutineScope.launch {
            try {
                val newFile = if (!positioningLogsFile.exists()) {
                    positioningLogsFile.createNewFile()
                    true
                } else {
                    false
                }
                BufferedWriter(FileWriter(positioningLogsFile, true)).use { writer ->
                    if (newFile) {
                        writer.write(formattedPositioningColumns)
                    }
                    writer.append(formattedValues)
                }
            } catch (exception: IOException) {
                handleAppendLogException(exception)
            }
        }
    }

    private suspend fun handleAppendLogException(
        exception: Exception,
    ) {
        Log.e(TAG, exception.localizedMessage, exception)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.logs_saving_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun appendFingerprintingLog(
        beacons: List<Beacon>,
    ) {
        radioMap[System.currentTimeMillis()] = beacons
            .associate { it.bluetoothAddress to it.rssi }
    }

    fun shareLogs(
        activity: Activity,
    ) {
        coroutineScope.launch {
            saveRadioMapToFile()
            withContext(Dispatchers.Main) {
                try {
                    val uris = arrayListOf<Uri>()
                    positioningLogsFile.toFileUri()?.let { uris.add(it) }
                    fingerprintingLogsFile.toFileUri()?.let { uris.add(it) }
                    if (uris.isEmpty()) {
                        showLogsSharingErrorToast(R.string.no_logs_to_share)
                        return@withContext
                    }
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        type = "text/tab-separated-value"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooserIntent = Intent
                        .createChooser(
                            shareIntent,
                            context.getString(R.string.logs_sharing_title)
                        )
                    activity.startActivity(chooserIntent)
                } catch (exception: Exception) {
                    Log.e(TAG, exception.localizedMessage, exception)
                    showLogsSharingErrorToast()
                }
            }
        }
    }

    private fun File.toFileUri(): Uri? =
        if (length() > 0) {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", this)
        } else {
            null
        }

    private fun showLogsSharingErrorToast(
        @StringRes stringResId: Int = R.string.logs_sharing_error,
    ) {
        Toast.makeText(context, stringResId, Toast.LENGTH_SHORT).show()
    }

    private suspend fun saveRadioMapToFile() {
        if (radioMap.isEmpty()) return
        return withContext(Dispatchers.IO) {
            try {
                val macAddresses = radioMap.values.flatMap { it.keys }.toSet()
                val formattedFingerprintingColumns =
                    FingerprintingLogColumns.TIMESTAMP.value + "\t" +
                            macAddresses.joinToString("\t") + "\n"
                BufferedWriter(FileWriter(fingerprintingLogsFile)).use { writer ->
                    writer.write(formattedFingerprintingColumns)
                    radioMap.forEach { (timestamp, beaconData) ->
                        val formattedValues = macAddresses.joinToString("\t") { mac ->
                            beaconData[mac]?.toString() ?: ""
                        }
                        writer.append("$timestamp\t$formattedValues\n")
                    }
                }
            } catch (exception: IOException) {
                handleAppendLogException(exception)
            }
        }
    }

    // Loading existing logs
//    private suspend fun saveRadioMapToFile(): Boolean {
//        if (radioMap.isEmpty()) return true
//        return withContext(Dispatchers.IO) {
//            try {
//                val macAddresses = mutableSetOf<String>()
//                if (fingerprintingLogsFile.exists()) {
//                    macAddresses.addAll(loadRadioMapFromFile())
//                }
//                macAddresses.addAll(radioMap.values.flatMap { it.keys })
//                val formattedFingerprintingColumns =
//                    FingerprintingLogColumns.TIMESTAMP.value + "\t" +
//                            macAddresses.joinToString("\t") + "\n"
//                BufferedWriter(FileWriter(fingerprintingLogsFile)).use { writer ->
//                    writer.write(formattedFingerprintingColumns)
//                    radioMap.forEach { (timestamp, beaconData) ->
//                        val formattedValues = macAddresses.joinToString("\t") { mac ->
//                            beaconData[mac]?.toString() ?: ""
//                        }
//                        writer.append("$timestamp\t$formattedValues\n")
//                    }
//                }
//                true
//            } catch (exception: IOException) {
//                handleAppendLogException(exception)
//                false
//            }
//        }
//    }

//    private fun loadRadioMapFromFile(): Set<String> {
//        val macAddresses = mutableSetOf<String>()
//        fingerprintingLogsFile
//            .bufferedReader()
//            .useLines { lines ->
//                val header = lines.firstOrNull()
//                header
//                    ?.split("\t")
//                    ?.drop(1)
//                    ?.let { macAddress ->
//                        macAddresses.addAll(macAddress)
//                    }
//                lines
//                    .drop(1)
//                    .forEach { line ->
//                        val parts = line.split("\t")
//                        val timestamp = parts[0].toLong()
//                        val beaconData = parts
//                            .drop(1)
//                            .mapIndexedNotNull { index, rssi ->
//                                if (rssi.isNotEmpty()) {
//                                    macAddresses.elementAt(index) to rssi.toInt()
//                                } else {
//                                    null
//                                }
//                            }.toMap()
//                        radioMap[timestamp] = beaconData
//                    }
//            }
//        return macAddresses
//    }
}
