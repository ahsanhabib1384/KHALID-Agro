package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.data.AiCropDoctorRepository
import com.example.data.StructuredCropDiagnosisResult
import com.example.model.CropDiagnosis
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface CropDoctorAiState {
    data object Idle : CropDoctorAiState
    data object Loading : CropDoctorAiState
    data class Success(val result: StructuredCropDiagnosisResult) : CropDoctorAiState
    data class Error(val message: String) : CropDoctorAiState
}

sealed interface KrishiStreamState {
    data object Idle : KrishiStreamState
    data object Loading : KrishiStreamState
    data class Streaming(val text: String) : KrishiStreamState
    data class Success(val text: String) : KrishiStreamState
    data class Error(val message: String) : KrishiStreamState
}

class CropDoctorViewModel(
    private val aiRepo: AiCropDoctorRepository,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "CropDoctorVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _diagnosisState = MutableStateFlow<CropDoctorAiState>(CropDoctorAiState.Idle)
    val diagnosisState: StateFlow<CropDoctorAiState> = _diagnosisState.asStateFlow()

    private val _krishiStreamState = MutableStateFlow<KrishiStreamState>(KrishiStreamState.Idle)
    val krishiStreamState: StateFlow<KrishiStreamState> = _krishiStreamState.asStateFlow()

    val historyState: StateFlow<UiState<List<CropDiagnosis>>> =
        aiRepo.observeSavedDiagnoses(currentUserId)
            .map<List<CropDiagnosis>, UiState<List<CropDiagnosis>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing diagnosis history", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    fun analyzeCrop(
        cropName: String,
        symptoms: String,
        photoBitmap: Bitmap?,
        language: AppLanguage
    ) {
        if (cropName.isBlank() && photoBitmap == null) {
            _diagnosisState.value = CropDoctorAiState.Error(
                if (language == AppLanguage.BN) "ফসলের নাম বা ছবি দিন।" else "Please enter crop name or select a photo."
            )
            return
        }
        viewModelScope.launch {
            _diagnosisState.value = CropDoctorAiState.Loading
            try {
                val res = aiRepo.diagnoseCrop(
                    cropName = cropName.ifBlank { "Crop Leaf / Plant" },
                    symptoms = symptoms.ifBlank { "Visual symptom inspection from photo" },
                    photoBitmap = photoBitmap,
                    language = language
                )
                _diagnosisState.value = CropDoctorAiState.Success(res)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI crop diagnosis timed out", e)
                _diagnosisState.value = CropDoctorAiState.Error(
                    "Analysis timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI crop diagnosis failed", e)
                _diagnosisState.value = CropDoctorAiState.Error(
                    e.localizedMessage ?: "Crop analysis failed."
                )
            }
        }
    }

    fun askKrishiHelp(question: String, language: AppLanguage) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _krishiStreamState.value = KrishiStreamState.Loading
            var lastText = ""
            try {
                aiRepo.streamKrishiHelp(question, language).collect { partial ->
                    lastText = partial
                    _krishiStreamState.value = KrishiStreamState.Streaming(partial)
                }
                if (lastText.isNotBlank()) {
                    _krishiStreamState.value = KrishiStreamState.Success(lastText)
                } else {
                    _krishiStreamState.value = KrishiStreamState.Error("No response received.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Krishi help timed out", e)
                _krishiStreamState.value = KrishiStreamState.Error("Response timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Krishi help stream failed", e)
                _krishiStreamState.value = KrishiStreamState.Error(e.localizedMessage ?: "Krishi Help error.")
            }
        }
    }
}
