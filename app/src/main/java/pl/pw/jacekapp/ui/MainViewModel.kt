package pl.pw.jacekapp.ui

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonSyntaxException
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import pl.pw.jacekapp.R
import pl.pw.jacekapp.common.utils.BeaconUtils
import pl.pw.jacekapp.common.utils.ErrorHandler
import pl.pw.jacekapp.common.utils.Logger
import pl.pw.jacekapp.common.utils.QrCodeScannerUtils
import pl.pw.jacekapp.data.model.DataDownloadingStatus
import pl.pw.jacekapp.data.model.PositioningLogEntry
import pl.pw.jacekapp.data.model.QrCodeData
import pl.pw.jacekapp.data.model.TagType
import pl.pw.jacekapp.data.repository.QrNfcPositioningRepo
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val qrNfcPositioningRepo: QrNfcPositioningRepo,
    private val qrCodeScannerUtils: QrCodeScannerUtils,
    private val beaconUtils: BeaconUtils,
    private val errorHandler: ErrorHandler,
    private val logger: Logger,
) : ViewModel() {

    companion object {
        private const val TAG = "pw.MainViewModel"
    }

    val positioningLogEntry: PositioningLogEntry?
        get() = _positioningLogEntry
    val qrCodeData: LiveData<QrCodeData?>
        get() = _qrCodeData
    var shouldCollectFingerprints = true
    private var qrScanningInProgress = false
    private var _positioningLogEntry: PositioningLogEntry? = null
    private val _qrCodeData = MutableLiveData<QrCodeData?>()

    fun downloadQrCodesData(): Flow<DataDownloadingStatus> =
        qrNfcPositioningRepo.downloadQrCodesData()

    fun launchGmsQrCodeScanner(
        activity: Activity,
        errorCallback: () -> Unit,
    ) {
        if (qrScanningInProgress) {
            return
        }
        qrScanningInProgress = true
        qrCodeScannerUtils.setUpGmsBarcodeScanningApi(activity)
        qrCodeScannerUtils.checkGmsBarcodeScanningModuleAvailability(
            successCallback = { moduleAvailabilityResponse ->
                if (moduleAvailabilityResponse.areModulesAvailable()) {
                    launchGmsQrCodeScanner()
                } else {
                    installGmsBarcodeScanningModule(errorCallback)
                }
            },
            errorCallback = { exception ->
                handleQrScanningException(exception)
            }
        )
    }

    private fun launchGmsQrCodeScanner() {
        qrCodeScannerUtils.startScanViaGmsBarcodeScanner()
            .addOnSuccessListener { qrCode ->
                handleQrContent(qrCode.rawValue)
            }
            .addOnCanceledListener {
                cancelQrScanning()
            }
            .addOnFailureListener { exception ->
                handleQrScanningException(exception)
            }
    }

    fun handleQrContent(
        qrContent: String?,
    ) {
        if (qrContent.isNullOrBlank()) {
            errorHandler.handleError(R.string.invalid_qr_format, TAG, "Empty QR content.")
            qrScanningInProgress = false
        } else {
            handleQrIdFromUrl(qrContent)
        }
    }

    private fun handleQrIdFromUrl(
        url: String,
    ) {
        val qrText = try {
            url.substringAfterLast("/")
        } catch (exception: JsonSyntaxException) {
            handleQrScanningException(exception)
            return
        }
        val qrCodeData = qrNfcPositioningRepo.qrCodesData.firstOrNull { it.qrText == qrText }
        if (qrCodeData == null) {
            errorHandler.handleError(
                R.string.invalid_qr_format,
                TAG,
                "Invalid QR value - $qrText"
            )
        } else {
            _qrCodeData.value = qrCodeData
        }
        qrScanningInProgress = false
    }

    fun updateLogs(
        qrCodeData: QrCodeData,
        shouldAppend: Boolean,
    ) {
        initPositioningLogEntryIfNeeded()
        _positioningLogEntry?.qrScanTimestamp = System.currentTimeMillis()
        _positioningLogEntry?.qrPosition = "${qrCodeData.x} ${qrCodeData.y}"
        if (shouldAppend) {
            appendPositioningLog()
        }
    }

    private fun appendPositioningLog() {
        _positioningLogEntry?.let { logger.appendPositioningLog(it) }
        _positioningLogEntry = null
    }

    private fun initPositioningLogEntryIfNeeded() {
        if (_positioningLogEntry == null) {
            _positioningLogEntry = PositioningLogEntry()
        }
    }

    fun updateLogs(
        tagType: TagType,
    ) {
        initPositioningLogEntryIfNeeded()
        _positioningLogEntry?.tagTimestamp = System.currentTimeMillis()
        _positioningLogEntry?.tagType = tagType
        if (tagType != TagType.STOP_DYNAMIC) {
            appendPositioningLog()
        }
    }

    private fun handleQrScanningException(
        exception: Exception? = null,
    ) {
        errorHandler.handleError(R.string.qr_scanning_error, TAG, exception?.localizedMessage)
        cancelQrScanning()
    }

    fun cancelQrScanning() {
        Log.d(TAG, "Canceling scanner")
        qrScanningInProgress = false
    }

    private fun installGmsBarcodeScanningModule(
        errorCallback: () -> Unit,
    ) {
        qrCodeScannerUtils.installGmsBarcodeScanningModule(
            successCallback = {
                launchGmsQrCodeScanner()
            }, errorCallback = { exception ->
                Log.e(TAG, exception.localizedMessage, exception)
                errorCallback()
            })
    }

    fun startScanViaZxingScanner(
        zxingScannerLauncher: ActivityResultLauncher<ScanOptions>,
    ) {
        qrCodeScannerUtils.startScanViaZxingScanner(
            zxingScannerLauncher,
            errorCallback = { exception ->
                handleQrScanningException(exception)
            })
    }

    fun shareLogs(
        activity: Activity,
    ) {
        logger.shareLogs(activity)
    }

    fun listenForScannedBeacons() {
        beaconUtils.listenForScannedBeacons { beacons ->
            if (shouldCollectFingerprints) {
                logger.appendFingerprintingLog(beacons)
            }
        }
    }

    fun startRangingBeacons() {
        beaconUtils.startRangingBeacons()
    }

    fun stopRangingBeacons() {
        beaconUtils.stopRangingBeacons()
    }
}
