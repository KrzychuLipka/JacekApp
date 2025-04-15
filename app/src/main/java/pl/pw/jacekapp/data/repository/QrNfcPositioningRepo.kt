package pl.pw.jacekapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import pl.pw.jacekapp.data.model.DataDownloaded
import pl.pw.jacekapp.data.model.DataDownloading
import pl.pw.jacekapp.data.model.DataDownloadingError
import pl.pw.jacekapp.data.model.DataDownloadingStatus
import pl.pw.jacekapp.data.model.QrCodeData
import pl.pw.jacekapp.data.repository.dto.FetchQrCodesResponseDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrNfcPositioningRepo @Inject constructor(
    private val positioningApi: QrPositioningApi,
) {

    val qrCodesData: List<QrCodeData>
        get() = _qrCodesData
    private var _qrCodesData = emptyList<QrCodeData>()

    fun downloadQrCodesData(): Flow<DataDownloadingStatus> = flow {
        emit(DataDownloading)
        val response = positioningApi.fetchQrCodes()
        if (!response.isSuccessful) {
            emit(DataDownloadingError("Fetching QR codes error (${response.code()})"))
            return@flow
        }
        val qrCodeData = response.getQrCodesData()
        if (qrCodeData.isEmpty()) {
            emit(DataDownloadingError("Invalid server response"))
            return@flow
        }
        _qrCodesData = qrCodeData
        emit(DataDownloaded)
    }.catch { exception ->
        emit(DataDownloadingError(exception.localizedMessage ?: "$exception"))
    }

    private fun Response<FetchQrCodesResponseDto>.getQrCodesData(): List<QrCodeData> =
        body()
            ?.features
            ?.filter {
                val geometry = it?.geometry
                geometry?.x != null && geometry.y != null && it.attributes.qrText != null
            }
            ?.map {
                QrCodeData(
                    qrText = it?.attributes?.qrText ?: "",
                    x = it?.geometry?.x ?: 0.0,
                    y = it?.geometry?.y ?: 0.0
                )
            }
            ?: emptyList()
}
