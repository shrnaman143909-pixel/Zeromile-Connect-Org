package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * Metadata record corresponding to 'complaint_evidence' table in Supabase.
 */
@JsonClass(generateAdapter = true)
data class ComplaintEvidenceRecord(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "complaint_id") val complaintId: String,
    @Json(name = "uploaded_by") val uploadedBy: String,
    @Json(name = "storage_path") val storagePath: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "file_size") val fileSize: Long,
    @Json(name = "created_at") val createdAt: String? = null,
    // Transient field for secured image preview in UI
    @Json(name = "signed_url") val signedUrl: String? = null
)

/**
 * Payload sent to Supabase REST endpoint to insert evidence metadata.
 */
@JsonClass(generateAdapter = true)
data class CreateEvidencePayload(
    @Json(name = "complaint_id") val complaintId: String,
    @Json(name = "uploaded_by") val uploadedBy: String,
    @Json(name = "storage_path") val storagePath: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "file_size") val fileSize: Long
)

/**
 * Payload sent to Supabase Storage signing endpoint: POST /storage/v1/object/sign/{bucket}/{path}
 */
@JsonClass(generateAdapter = true)
data class SignUrlPayload(
    @Json(name = "expiresIn") val expiresIn: Int = 3600
)

@JsonClass(generateAdapter = true)
data class SignUrlResponse(
    @Json(name = "signedURL") val signedURL: String
)

/**
 * Client-side staged evidence item before / during submission.
 */
enum class EvidenceUploadState {
    STAGED,
    UPLOADING,
    UPLOADED,
    FAILED
}

data class StagedEvidenceItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String? = null,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    val bytes: ByteArray? = null,
    val state: EvidenceUploadState = EvidenceUploadState.STAGED,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null,
    val storagePath: String? = null
) {
    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "Photo"
            val kb = fileSizeBytes / 1024.0
            return if (kb > 1024) {
                String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
}

/**
 * Structured location container for civic complaints.
 */
data class ComplaintLocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Double? = null,
    val source: String = "manual", // 'gps', 'manual', 'address', 'ai_detected'
    val locationText: String = "",
    val wardName: String? = null,
    val wardNumber: String? = null,
    val wardId: String? = null,
    val landmark: String? = null
)
