package pl.pw.jacekapp.data.model

data class PositioningLogEntry(
    var qrScanTimestamp: Long? = null,
    var qrPosition: String? = null,
    var tagTimestamp: Long? = null,
    var tagType: TagType? = null,
)
