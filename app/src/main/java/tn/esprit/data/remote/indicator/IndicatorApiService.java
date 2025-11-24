package tn.esprit.data.remote.indicator;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.indicator.IndicatorType;
import tn.esprit.domain.indicator.PatientIndicator;

/**
 * Retrofit API for indicator-related endpoints.
 *
 * Matches backend:
 *  - GET /indicator-types
 *  - GET /indicators/me
 *  - POST /indicators/me
 *  - DELETE /indicators/me/{id}
 */
public interface IndicatorApiService {

    /**
     * GET /indicator-types
     *
     * Returns the static catalog of available indicator types.
     * (If you later require auth for this, we can add Authorization header.)
     */
    @GET("indicator-types")
    Call<ListResponseDto<IndicatorType>> getIndicatorTypes();

    /**
     * GET /indicators/me
     *
     * Lists indicators of the current patient.
     *
     * Query params (all optional):
     *  - indicatorTypeId
     *  - from (ISO-8601 date-time string)
     *  - to   (ISO-8601 date-time string)
     */
    @GET("indicators/me")
    Call<ListResponseDto<PatientIndicator>> getMyIndicators(
            @Header("Authorization") String authorization,
            @Query("indicatorTypeId") Long indicatorTypeId,
            @Query("from") String fromIso,
            @Query("to") String toIso
    );

    /**
     * POST /indicators/me
     *
     * Current patient adds a new indicator measurement.
     */
    @POST("indicators/me")
    Call<PatientIndicator> addMyIndicator(
            @Header("Authorization") String authorization,
            @Body PatientIndicatorCreateRequestDto request
    );

    /**
     * DELETE /indicators/me/{id}
     *
     * Current patient deletes one of their indicators by id.
     */
    @DELETE("indicators/me/{id}")
    Call<Void> deleteMyIndicator(
            @Header("Authorization") String authorization,
            @Path("id") Long indicatorId
    );

    /**
     * Request body for POST /indicators/me.
     * Mirrors backend dto.indicator.PatientIndicatorCreateRequest.
     */
    class PatientIndicatorCreateRequestDto {

        private Long indicatorTypeId;   // required

        private BigDecimal numericValue; // optional; at least one of numeric/text
        private String textValue;

        /**
         * Optional ISO-8601 timestamp string.
         * If null, backend will use now().
         */
        private String measuredAt;

        private String note;

        public PatientIndicatorCreateRequestDto() {
        }

        public Long getIndicatorTypeId() {
            return indicatorTypeId;
        }

        public void setIndicatorTypeId(Long indicatorTypeId) {
            this.indicatorTypeId = indicatorTypeId;
        }

        public BigDecimal getNumericValue() {
            return numericValue;
        }

        public void setNumericValue(BigDecimal numericValue) {
            this.numericValue = numericValue;
        }

        public String getTextValue() {
            return textValue;
        }

        public void setTextValue(String textValue) {
            this.textValue = textValue;
        }

        public String getMeasuredAt() {
            return measuredAt;
        }

        public void setMeasuredAt(String measuredAt) {
            this.measuredAt = measuredAt;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
