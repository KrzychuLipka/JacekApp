package pl.pw.jacekapp.data.repository.dto

import com.google.gson.annotations.SerializedName

data class AttributesDto(
    val id: Long?,
    @SerializedName("qr_text")
    val qrText: String?,
    @SerializedName("building_id")
    val buildingId: Long?,
    @SerializedName("poziom")
    val level: Int?,
    @SerializedName("floor_id")
    val floorId: Long?,
)
