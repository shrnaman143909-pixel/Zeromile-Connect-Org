package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Phase 8 Citizen Notification Record
 * Corresponds to public.notifications in PostgreSQL / Supabase
 */
@JsonClass(generateAdapter = true)
data class NotificationRecord(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "complaint_id") val complaintId: String? = null,
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "idempotency_key") val idempotencyKey: String? = null
)

/**
 * Standard notification types for Zeromile Connect
 */
enum class NotificationType(val rawValue: String) {
    STATUS_UPDATE("status_update"),
    ASSIGNMENT("assignment"),
    OFFICIAL_UPDATE("official_update"),
    RESOLUTION("resolution"),
    CLOSURE("closure"),
    SYSTEM("system");

    companion object {
        fun fromString(value: String?): NotificationType {
            return entries.find { it.rawValue.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

/**
 * Payload for updating notification read status via REST
 */
@JsonClass(generateAdapter = true)
data class UpdateNotificationReadPayload(
    @Json(name = "read") val read: Boolean
)

/**
 * Payload for creating notification (used by admin/trusted server flow)
 */
@JsonClass(generateAdapter = true)
data class CreateNotificationPayload(
    @Json(name = "user_id") val userId: String,
    @Json(name = "complaint_id") val complaintId: String? = null,
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "idempotency_key") val idempotencyKey: String? = null
)

/**
 * Transient live toast display model for real-time alerts
 */
data class LiveToastNotification(
    val id: String,
    val title: String,
    val message: String,
    val complaintId: String? = null,
    val type: NotificationType = NotificationType.STATUS_UPDATE,
    val timestamp: Long = System.currentTimeMillis()
)
