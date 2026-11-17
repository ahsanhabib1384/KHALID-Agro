package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.R
import com.example.core.AppLanguage
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.CropDiagnosis
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

data class StructuredCropDiagnosisResult(
    val diagnosis: String,
    val severity: String,
    val treatmentSteps: String,
    val preventiveAdvice: String
)

class AiCropDoctorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        private const val TAG = "AiCropDoctorRepo"
    }

    private val diagnosisSchema = Schema.obj(
        properties = mapOf(
            "diagnosis" to Schema.string(description = "Likely crop disease, pest, or nutrient deficiency"),
            "severity" to Schema.enumeration(
                values = listOf("LOW", "MODERATE", "HIGH"),
                description = "Severity level of the crop issue"
            ),
            "treatmentSteps" to Schema.string(description = "Practical step-by-step organic and safe treatment advice for Bangladesh farmers"),
            "preventiveAdvice" to Schema.string(description = "Preventive tips for upcoming seasons in Bangladesh")
        )
    )

    private val diagnosisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosisSchema
            }
        )
    }

    private val krishiHelpModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun observeSavedDiagnoses(uid: String): Flow<List<CropDiagnosis>> = flow {
        val path = "users/$uid/crop_diagnoses"
        emitAll(
            db.collection("users").document(uid).collection("crop_diagnoses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CropDiagnosis::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun diagnoseCrop(
        cropName: String,
        symptoms: String,
        photoBitmap: Bitmap?,
        language: AppLanguage
    ): StructuredCropDiagnosisResult {
        val langInstruction = if (language == AppLanguage.BN) {
            "Respond in clear, simple Bengali (বাংলা) suitable for farmers in rural Bangladesh."
        } else {
            "Respond in clear, practical English tailored for Bangladesh agriculture."
        }
        val promptText = """
            You are an expert agronomist for Khalid Agro in Chuadanga, Bangladesh.
            Analyze this crop problem and return structured guidance.
            Crop: $cropName
            Observed Symptoms: $symptoms
            $langInstruction
        """.trimIndent()

        val response = withTimeout(60_000L) {
            if (photoBitmap != null) {
                diagnosisModel.generateContent(
                    content {
                        image(photoBitmap)
                        text(promptText)
                    }
                )
            } else {
                diagnosisModel.generateContent(promptText)
            }
        }

        val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
        val json = JSONObject(jsonText)
        val result = StructuredCropDiagnosisResult(
            diagnosis = json.getString("diagnosis"),
            severity = json.optString("severity", "MODERATE"),
            treatmentSteps = json.getString("treatmentSteps"),
            preventiveAdvice = json.optString("preventiveAdvice", "")
        )

        // Persist diagnosis in Firestore user history
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            val diagId = "diag_${UUID.randomUUID().toString().take(12)}"
            try {
                val combinedTreatment = buildString {
                    append(result.treatmentSteps)
                    if (result.preventiveAdvice.isNotBlank()) {
                        append("\n\n")
                        append(result.preventiveAdvice)
                    }
                }.take(1450)
                val record = CropDiagnosis(
                    cropName = cropName.take(115),
                    symptoms = symptoms.ifBlank { "Photo inspection" }.take(750),
                    diagnosis = result.diagnosis.take(1450),
                    treatment = combinedTreatment,
                    severity = result.severity.take(35)
                )
                db.collection("users").document(uid)
                    .collection("crop_diagnoses").document(diagId)
                    .set(record.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist diagnosis to Firestore", e)
                handleFirestoreError(e, OperationType.CREATE, "users/$uid/crop_diagnoses/$diagId")
            }
        }

        return result
    }

    fun streamKrishiHelp(question: String, language: AppLanguage): Flow<String> = flow {
        val langNote = if (language == AppLanguage.BN) {
            "উত্তর সহজ ও পরিষ্কার বাংলায় দিন।"
        } else {
            "Answer in clear, farmer-friendly English."
        }
        val prompt = """
            You are Khalid Agro Krishi Help advisor supporting farmers and livestock keepers in Bangladesh (especially Khulna/Chuadanga region).
            Keep advice practical, seasonal, and safe. $langNote
            Question: $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            krishiHelpModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI advisor returned an empty response.")
        }
    }
}
