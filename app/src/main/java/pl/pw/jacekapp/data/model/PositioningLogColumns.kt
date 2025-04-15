package pl.pw.jacekapp.data.model

enum class PositioningLogColumns(
    val value: String,
) {
    QR_SCAN_TIMESTAMP("qr_scan_timestamp"),
    QR_POSITION("qr_position"),
    TAG_TIMESTAMP("tag_timestamp"),
    TAG("tag"),
}
