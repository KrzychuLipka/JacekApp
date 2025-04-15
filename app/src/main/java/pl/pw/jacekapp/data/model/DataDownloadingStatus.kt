package pl.pw.jacekapp.data.model

sealed class DataDownloadingStatus

data object DataDownloading : DataDownloadingStatus()

data object DataDownloaded : DataDownloadingStatus()

class DataDownloadingError(
    val message: String
) : DataDownloadingStatus()
