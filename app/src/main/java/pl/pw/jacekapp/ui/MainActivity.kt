package pl.pw.jacekapp.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.pw.jacekapp.R
import pl.pw.jacekapp.common.extensions.allPermissionsGranted
import pl.pw.jacekapp.common.extensions.getMultiplePermissionsLauncher
import pl.pw.jacekapp.common.extensions.listenForConnectionChanges
import pl.pw.jacekapp.common.extensions.scanBeaconsPermissions
import pl.pw.jacekapp.common.extensions.unregisterConnectivityStateReceiverIfNeeded
import pl.pw.jacekapp.data.model.DataDownloaded
import pl.pw.jacekapp.data.model.DataDownloading
import pl.pw.jacekapp.data.model.DataDownloadingError
import pl.pw.jacekapp.data.model.TagType

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val progressBar: ProgressBar by lazy {
        findViewById(R.id.progress_bar)
    }
    private val feedbackTextView: TextView by lazy {
        findViewById(R.id.feedback_text_view)
    }
    private val scanQrButton: Button by lazy {
        findViewById(R.id.scan_qr_button)
    }
    private val shareLogsButton: FloatingActionButton by lazy {
        findViewById(R.id.share_logs_button)
    }
    private val startStaticScanButton: Button by lazy {
        findViewById(R.id.start_static_scan_button)
    }
    private val stopStaticScanButton: Button by lazy {
        findViewById(R.id.stop_static_scan_button)
    }
    private val startDynamicScanButton: Button by lazy {
        findViewById(R.id.start_dynamic_scan_button)
    }
    private val stopDynamicScanButton: Button by lazy {
        findViewById(R.id.stop_dynamic_scan_button)
    }
    private val zxingScannerLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        val qrContent = result.contents
        if (qrContent.isNullOrBlank()) {
            updateFeedback(R.string.positioning_canceled)
            viewModel.cancelQrScanning()
        } else {
            viewModel.handleQrContent(qrContent)
        }
    }
    private val requestPermissionLauncher = getMultiplePermissionsLauncher {
        startRangingBeaconsIfPossible()
    }

    private fun startRangingBeaconsIfPossible() {
        listenForConnectionChanges {
            viewModel.startRangingBeacons()
        }
    }

    private fun updateFeedback(
        @StringRes stringResId: Int,
    ) {
        updateFeedback(getString(stringResId))
    }

    private fun updateFeedback(
        message: String,
    ) {
        feedbackTextView.text = message
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setUpUI()
        downloadQrCodesData()
        observeQrCodeData()
        viewModel.listenForScannedBeacons()
        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        if (allPermissionsGranted(scanBeaconsPermissions)) {
            startRangingBeaconsIfPossible()
        } else {
            requestPermissionLauncher.launch(scanBeaconsPermissions)
        }
    }

    private fun observeQrCodeData() {
        viewModel.qrCodeData.observe(this) { qrCodeData ->
            if (qrCodeData == null) return@observe
            val dynamicMeasureFinished =
                viewModel.positioningLogEntry?.tagType == TagType.STOP_DYNAMIC
            if (dynamicMeasureFinished) {
                updateFeedback(R.string.dynamic_measure_finished)
                scanQrButton.activate()
            } else {
                updateFeedback(getString(R.string.user_position, "${qrCodeData.x} ${qrCodeData.y}"))
                scanQrButton.deactivate()
                startStaticScanButton.activate()
                startDynamicScanButton.activate()
            }
            viewModel.updateLogs(qrCodeData, dynamicMeasureFinished)
        }
    }

    private fun downloadQrCodesData() {
        lifecycleScope.launch {
            viewModel
                .downloadQrCodesData()
                .collect { status ->
                    when (status) {
                        DataDownloading -> {
                            progressBar.visibility = View.VISIBLE
                        }

                        DataDownloaded -> {
                            progressBar.visibility = View.GONE
                        }

                        is DataDownloadingError -> {
                            progressBar.visibility = View.GONE
                            showTryAgainSnackBar()
                        }
                    }
                }
        }
    }

    private fun showTryAgainSnackBar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.downloading_qr_codes_error),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(getString(R.string.try_again_button)) {
            downloadQrCodesData()
        }.show()
    }

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateFeedback(R.string.test_start_info)
        scanQrButton.setOnClickListener {
            launchQrScanner()
        }
        shareLogsButton.setOnClickListener {
            viewModel.shareLogs(this)
        }
        startStaticScanButton.setOnClickListener {
            handleStartStaticScanButtonClick()
        }
        stopStaticScanButton.setOnClickListener {
            handleStopStaticScanButtonClick()
        }
        startDynamicScanButton.setOnClickListener {
            handleStartDynamicScanButtonClick()
        }
        stopDynamicScanButton.setOnClickListener {
            handleStopDynamicScanButtonClick()
        }
    }

    private fun handleStopDynamicScanButtonClick() {
        updateLogs(TagType.STOP_DYNAMIC)
        stopDynamicScanButton.deactivate()
        scanQrButton.activate()
    }

    private fun updateLogs(
        tagType: TagType,
    ) {
        val activeAction = when (tagType) {
            TagType.START_STATIC -> R.string.static_measure_started
            TagType.STOP_STATIC -> R.string.static_measure_stopped
            TagType.START_DYNAMIC -> R.string.dynamic_measure_started
            TagType.STOP_DYNAMIC -> R.string.dynamic_measure_path_end
        }
        updateFeedback(activeAction)
        viewModel.updateLogs(tagType)
    }

    private fun handleStartDynamicScanButtonClick() {
        updateLogs(TagType.START_DYNAMIC)
        stopDynamicScanButton.activate()
        startStaticScanButton.deactivate()
        startDynamicScanButton.deactivate()
    }

    private fun handleStartStaticScanButtonClick() {
        updateLogs(TagType.START_STATIC)
        stopStaticScanButton.activate()
        startStaticScanButton.deactivate()
        startDynamicScanButton.deactivate()
    }

    private fun handleStopStaticScanButtonClick() {
        updateLogs(TagType.STOP_STATIC)
        stopStaticScanButton.deactivate()
        scanQrButton.activate()
    }

    private fun Button.deactivate() {
        isEnabled = false
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
        setTextColor(ContextCompat.getColor(context, R.color.gray_dark))
    }

    private fun Button.activate() {
        isEnabled = true
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        setTextColor(ContextCompat.getColor(context, R.color.yellow))
    }

    private fun launchQrScanner() {
        viewModel.launchGmsQrCodeScanner(this, errorCallback = {
            viewModel.startScanViaZxingScanner(zxingScannerLauncher)
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.shouldCollectFingerprints = true
    }

    override fun onPause() {
        super.onPause()
        viewModel.shouldCollectFingerprints = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterConnectivityStateReceiverIfNeeded()
        viewModel.stopRangingBeacons()
    }
}
