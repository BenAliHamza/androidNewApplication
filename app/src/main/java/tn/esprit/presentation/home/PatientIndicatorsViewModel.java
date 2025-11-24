package tn.esprit.presentation.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.indicator.PatientIndicatorRepository;
import tn.esprit.data.remote.indicator.IndicatorApiService.PatientIndicatorCreateRequestDto;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.indicator.IndicatorType;
import tn.esprit.domain.indicator.PatientIndicator;

/**
 * ViewModel for "My indicators" screen.
 *
 * - Loads indicator types (blood pressure, heart rate, etc.) from backend
 * - Loads patient indicators list (optionally filtered by type)
 * - Can add a new indicator (POST /indicators/me)
 * - Can delete an indicator (DELETE /indicators/me/{id})
 */
public class PatientIndicatorsViewModel extends AndroidViewModel {

    private final PatientIndicatorRepository patientIndicatorRepository;
    private final AuthLocalDataSource authLocalDataSource;

    private final MutableLiveData<List<PatientIndicator>> indicators =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<IndicatorType>> indicatorTypes =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>(null);

    // Event: whether last add operation succeeded
    private final MutableLiveData<Boolean> lastAddSuccess =
            new MutableLiveData<>(false);

    // Remember last used filter (null = "all types")
    @Nullable
    private Long lastIndicatorTypeIdFilter = null;

    public PatientIndicatorsViewModel(@NonNull Application application) {
        super(application);
        patientIndicatorRepository =
                new PatientIndicatorRepository(application.getApplicationContext());
        authLocalDataSource =
                new AuthLocalDataSource(application.getApplicationContext());
    }

    public LiveData<List<PatientIndicator>> getIndicators() {
        return indicators;
    }

    public LiveData<List<IndicatorType>> getIndicatorTypes() {
        return indicatorTypes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLastAddSuccess() {
        return lastAddSuccess;
    }

    public void clearLastAddSuccess() {
        lastAddSuccess.setValue(false);
    }

    // ---------------------------------------------------------------------
    // Public actions
    // ---------------------------------------------------------------------

    /**
     * Load all indicator types from backend (blood pressure, heart rate, etc.).
     */
    public void loadIndicatorTypes() {
        patientIndicatorRepository.getIndicatorTypes(new PatientIndicatorRepository.IndicatorTypesCallback() {
            @Override
            public void onSuccess(List<IndicatorType> types) {
                if (types == null) {
                    indicatorTypes.postValue(new ArrayList<>());
                } else {
                    indicatorTypes.postValue(new ArrayList<>(types));
                }
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                String msg = null;
                if (throwable != null) {
                    msg = "Network error while loading indicator types.";
                } else if (httpCode != null) {
                    msg = "Failed to load indicator types (code " + httpCode + ")";
                }
                if (errorBody != null && !errorBody.isEmpty()) {
                    if (msg == null) {
                        msg = errorBody;
                    } else {
                        msg = msg + " " + errorBody;
                    }
                }
                if (msg == null) {
                    msg = "Unknown error while loading indicator types.";
                }
                errorMessage.postValue(msg);
            }
        });
    }

    /**
     * Load indicators for current patient with no type filter.
     */
    public void loadIndicators() {
        loadIndicatorsInternal(null);
    }

    /**
     * Load indicators for current patient filtered by type.
     *
     * @param indicatorTypeId nullable. If null, loads all types.
     */
    public void loadIndicatorsForType(@Nullable Long indicatorTypeId) {
        loadIndicatorsInternal(indicatorTypeId);
    }

    /**
     * Explicitly report an error from UI, if ever needed.
     */
    public void reportError(@Nullable String msg) {
        errorMessage.setValue(msg);
    }

    /**
     * Add a new indicator for the current patient using POST /indicators/me.
     *
     * @param indicatorTypeId required
     * @param numericValue    optional, may be null
     * @param textValue       optional, may be null
     * @param note            optional, may be null
     */
    public void addIndicator(@NonNull Long indicatorTypeId,
                             @Nullable BigDecimal numericValue,
                             @Nullable String textValue,
                             @Nullable String note) {

        loading.setValue(true);
        errorMessage.setValue(null);
        lastAddSuccess.setValue(false);

        PatientIndicatorCreateRequestDto dto = new PatientIndicatorCreateRequestDto();
        dto.setIndicatorTypeId(indicatorTypeId);
        dto.setNumericValue(numericValue);
        dto.setTextValue(textValue);
        // Let backend use now() if measuredAt is null
        dto.setMeasuredAt(null);
        dto.setNote(note);

        String authorizationHeader = buildAuthorizationHeader();

        patientIndicatorRepository.addMyIndicator(
                authorizationHeader,
                dto,
                new PatientIndicatorRepository.AddIndicatorCallback() {
                    @Override
                    public void onSuccess(PatientIndicator created) {
                        // Refresh list with same filter
                        loadIndicatorsInternal(lastIndicatorTypeIdFilter);
                        lastAddSuccess.postValue(true);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);
                        lastAddSuccess.postValue(false);

                        String msg = null;

                        if (throwable != null) {
                            msg = "Network error while saving indicator.";
                        } else if (httpCode != null) {
                            msg = "Failed to save indicator (code " + httpCode + ")";
                        }

                        if (errorBody != null && !errorBody.isEmpty()) {
                            if (msg == null) {
                                msg = errorBody;
                            } else {
                                msg = msg + " " + errorBody;
                            }
                        }

                        if (msg == null) {
                            msg = "Unknown error while saving indicator.";
                        }

                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    /**
     * Delete an indicator for the current patient using DELETE /indicators/me/{id}.
     */
    public void deleteIndicator(@NonNull Long indicatorId) {
        loading.setValue(true);
        errorMessage.setValue(null);

        String authorizationHeader = buildAuthorizationHeader();

        patientIndicatorRepository.deleteMyIndicator(
                authorizationHeader,
                indicatorId,
                new PatientIndicatorRepository.DeleteIndicatorCallback() {
                    @Override
                    public void onSuccess() {
                        // Reload list with current filter
                        loadIndicatorsInternal(lastIndicatorTypeIdFilter);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);

                        String msg = null;

                        if (throwable != null) {
                            msg = "Network error while deleting indicator.";
                        } else if (httpCode != null) {
                            msg = "Failed to delete indicator (code " + httpCode + ")";
                        }

                        if (errorBody != null && !errorBody.isEmpty()) {
                            if (msg == null) {
                                msg = errorBody;
                            } else {
                                msg = msg + " " + errorBody;
                            }
                        }

                        if (msg == null) {
                            msg = "Unknown error while deleting indicator.";
                        }

                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    // ---------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------

    private void loadIndicatorsInternal(@Nullable Long indicatorTypeId) {
        loading.setValue(true);
        errorMessage.setValue(null);
        lastIndicatorTypeIdFilter = indicatorTypeId;

        String authorizationHeader = buildAuthorizationHeader();

        patientIndicatorRepository.getMyIndicators(
                authorizationHeader,
                indicatorTypeId,
                null,   // fromIso
                null,   // toIso
                new PatientIndicatorRepository.IndicatorsCallback() {
                    @Override
                    public void onSuccess(List<PatientIndicator> list) {
                        loading.postValue(false);
                        if (list == null) {
                            indicators.postValue(new ArrayList<>());
                        } else {
                            indicators.postValue(new ArrayList<>(list));
                        }
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);

                        String msg = null;

                        if (throwable != null) {
                            msg = "Network error while loading indicators.";
                        } else if (httpCode != null) {
                            msg = "Failed to load indicators (code " + httpCode + ")";
                        }

                        if (errorBody != null && !errorBody.isEmpty()) {
                            if (msg == null) {
                                msg = errorBody;
                            } else {
                                msg = msg + " " + errorBody;
                            }
                        }

                        if (msg == null) {
                            msg = "Unknown error while loading indicators.";
                        }

                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    /**
     * Builds Authorization header using locally stored tokens.
     * Returns something like "Bearer <accessToken>", or null if not available.
     */
    @Nullable
    private String buildAuthorizationHeader() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null) {
            return null;
        }

        String accessToken = tokens.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return null;
        }

        String type = tokens.getTokenType();
        if (type == null || type.trim().isEmpty()) {
            type = "Bearer";
        }

        return type + " " + accessToken;
    }
}
