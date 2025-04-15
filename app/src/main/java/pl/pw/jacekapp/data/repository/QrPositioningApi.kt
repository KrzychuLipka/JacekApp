package pl.pw.jacekapp.data.repository

import pl.pw.jacekapp.data.repository.dto.FetchQrCodesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface QrPositioningApi {

    companion object {
        private const val MAIN_BUILDING_ID = 39L
    }

    @GET("server/rest/services/SION2_Geoopisy/sion_topo_qrcode/MapServer/0/query")
    suspend fun fetchQrCodes(
        @Query("where") where: String = "1=1",
        @Query("building_id") buildingId: Long = MAIN_BUILDING_ID,
        @Query("text") text: String? = null,
        @Query("objectIds") objectIds: String? = null,
        @Query("time") time: String? = null,
        @Query("timeRelation") timeRelation: String = "esriTimeRelationOverlaps",
        @Query("geometry") geometry: String? = null,
        @Query("geometryType") geometryType: String = "esriGeometryEnvelope",
        @Query("inSR") inSR: String? = null,
        @Query("spatialRel") spatialRel: String = "esriSpatialRelIntersects",
        @Query("distance") distance: String? = null,
        @Query("units") units: String = "esriSRUnit_Foot",
        @Query("relationParam") relationParam: String? = null,
        @Query("outFields") outFields: String = "*",
        @Query("returnGeometry") returnGeometry: Boolean = true,
        @Query("returnTrueCurves") returnTrueCurves: Boolean = false,
        @Query("maxAllowableOffset") maxAllowableOffset: String? = null,
        @Query("geometryPrecision") geometryPrecision: String? = null,
        @Query("outSR") outSR: String? = null,
        @Query("havingClause") havingClause: String? = null,
        @Query("returnIdsOnly") returnIdsOnly: Boolean = false,
        @Query("returnCountOnly") returnCountOnly: Boolean = false,
        @Query("orderByFields") orderByFields: String? = null,
        @Query("groupByFieldsForStatistics") groupByFieldsForStatistics: String? = null,
        @Query("outStatistics") outStatistics: String? = null,
        @Query("returnZ") returnZ: Boolean = false,
        @Query("returnM") returnM: Boolean = false,
        @Query("gdbVersion") gdbVersion: String? = null,
        @Query("historicMoment") historicMoment: String? = null,
        @Query("returnDistinctValues") returnDistinctValues: Boolean = false,
        @Query("resultOffset") resultOffset: String? = null,
        @Query("resultRecordCount") resultRecordCount: String? = null,
        @Query("returnExtentOnly") returnExtentOnly: Boolean = false,
        @Query("sqlFormat") sqlFormat: String = "none",
        @Query("datumTransformation") datumTransformation: String? = null,
        @Query("parameterValues") parameterValues: String? = null,
        @Query("rangeValues") rangeValues: String? = null,
        @Query("quantizationParameters") quantizationParameters: String? = null,
        @Query("featureEncoding") featureEncoding: String = "esriDefault",
        @Query("f") format: String = "pjson"
    ): Response<FetchQrCodesResponseDto>
}
