package com.example.zeromile.data.evidence

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.zeromile.data.model.ComplaintEvidenceRecord
import com.example.zeromile.data.model.CreateEvidencePayload
import com.example.zeromile.data.model.SignUrlPayload
import com.example.zeromile.data.model.StagedEvidenceItem
import com.example.zeromile.data.remote.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.UUID

object EvidenceManager {
    private const val TAG = "EvidenceManager"
    const val BUCKET_NAME = "complaint-evidence"
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    const val MAX_FILES_PER_COMPLAINT = 5

    private val ALLOWED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    private val ALLOWED_EXTENSIONS = setOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    )

    /**
     * Sanitizes a file name to prevent directory traversal or unsafe characters.
     */
    fun sanitizeFileName(rawName: String): String {
        val extension = rawName.substringAfterLast('.', "jpg").lowercase().take(5)
        val nameWithoutExt = rawName.substringBeforeLast('.')
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .take(40)
            .ifBlank { "evidence" }

        val safeExt = if (extension in ALLOWED_EXTENSIONS) extension else "jpg"
        return "${nameWithoutExt}.$safeExt"
    }

    /**
     * Verifies file header magic bytes to prevent MIME type spoofing.
     */
    fun verifyImageMagicBytes(bytes: ByteArray): String? {
        if (bytes.size < 12) return null

        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return "image/png"
        }

        // WebP: RIFF ... WEBP
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }

        return null
    }

    /**
     * Reads URI bytes and stages an evidence item with validation.
     */
    suspend fun stageEvidenceFromUri(
        context: Context,
        uri: Uri,
        originalName: String? = null
    ): Result<StagedEvidenceItem> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read image from source"))
            }

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Selected file is empty"))
            }

            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("File size exceeds 10MB limit (${bytes.size / (1024 * 1024)}MB)")
                )
            }

            val clientMime = contentResolver.getType(uri) ?: "image/jpeg"
            val verifiedMime = verifyImageMagicBytes(bytes) ?: clientMime

            if (verifiedMime !in ALLOWED_MIME_TYPES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Unsupported file format ($verifiedMime). Only JPEG, PNG, and WebP are allowed.")
                )
            }

            val finalFileName = sanitizeFileName(originalName ?: "evidence_${System.currentTimeMillis()}.jpg")

            val staged = StagedEvidenceItem(
                id = UUID.randomUUID().toString(),
                uriString = uri.toString(),
                fileName = finalFileName,
                mimeType = verifiedMime,
                fileSizeBytes = bytes.size.toLong(),
                bytes = bytes
            )

            Result.success(staged)
        } catch (e: Exception) {
            Log.e(TAG, "Error staging evidence file", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads bytes to Supabase Storage private bucket and records metadata in complaint_evidence table.
     */
    suspend fun uploadToSupabaseStorage(
        api: SupabaseApi?,
        supabaseUrl: String,
        complaintId: String,
        userId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<ComplaintEvidenceRecord> = withContext(Dispatchers.IO) {
        val sanitized = sanitizeFileName(fileName)
        val fileUuid = UUID.randomUUID().toString()
        // Private bucket storage path: {user_id}/{complaint_id}/{uuid}-{sanitized_filename}
        val storagePath = "$userId/$complaintId/$fileUuid-$sanitized"

        if (api != null) {
            try {
                val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)

                // 1. Upload object to Storage
                val uploadResponse = api.uploadStorageObject(storagePath, requestBody)
                if (!uploadResponse.isSuccessful) {
                    val errBody = uploadResponse.errorBody()?.string() ?: ""
                    Log.w(TAG, "Storage upload response code: ${uploadResponse.code()}, error: $errBody")
                    return@withContext Result.failure(java.io.IOException("Storage upload failed (${uploadResponse.code()}): ${uploadResponse.message()} $errBody"))
                }

                // 2. Insert into complaint_evidence table
                val payload = CreateEvidencePayload(
                    complaintId = complaintId,
                    uploadedBy = userId,
                    storagePath = storagePath,
                    fileName = sanitized,
                    mimeType = mimeType,
                    fileSize = bytes.size.toLong()
                )

                val inserted = api.createEvidence(payload)
                val record = inserted.firstOrNull() ?: ComplaintEvidenceRecord(
                    id = fileUuid,
                    complaintId = complaintId,
                    uploadedBy = userId,
                    storagePath = storagePath,
                    fileName = sanitized,
                    mimeType = mimeType,
                    fileSize = bytes.size.toLong(),
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date())
                )

                // 3. Generate short-lived signed URL for UI preview
                val signedUrl = try {
                    val signRes = api.createSignedUrl(storagePath, SignUrlPayload(3600))
                    val rawUrl = signRes.signedURL
                    if (rawUrl.startsWith("http")) rawUrl
                    else "${supabaseUrl.trimEnd('/')}/storage/v1${if (rawUrl.startsWith("/")) "" else "/"}$rawUrl"
                } catch (e: Exception) {
                    null
                }

                return@withContext Result.success(record.copy(signedUrl = signedUrl))
            } catch (e: Exception) {
                Log.w(TAG, "Remote Supabase storage upload failed", e)
                return@withContext Result.failure(e)
            }
        }

        // Local / demo sandbox fallback
        val fallbackRecord = ComplaintEvidenceRecord(
            id = fileUuid,
            complaintId = complaintId,
            uploadedBy = userId,
            storagePath = storagePath,
            fileName = sanitized,
            mimeType = mimeType,
            fileSize = bytes.size.toLong(),
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            signedUrl = "data:$mimeType;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        )
        Result.success(fallbackRecord)
    }
}
