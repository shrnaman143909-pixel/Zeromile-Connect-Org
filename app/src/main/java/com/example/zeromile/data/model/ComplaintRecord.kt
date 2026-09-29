package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Civic Complaint persistence model corresponding to Supabase 'complaints' table.
 */
@JsonClass(generateAdapter = true)
data class ComplaintRecord(
    @Json(name = "id") val id: String,
    @Json(name = "complaint_number") val complaintNumber: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "department_id") val departmentId: String? = null,
    @Json(name = "description") val description: String,
    @Json(name = "original_transcript") val originalTranscript: String,
    @Json(name = "input_mode") val inputMode: String = "voice",
    @Json(name = "language") val language: String = "en-IN",
    @Json(name = "location_text") val locationText: String,
    @Json(name = "ward_id") val wardId: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "location_accuracy_meters") val locationAccuracyMeters: Double? = null,
    @Json(name = "location_source") val locationSource: String? = "manual",
    @Json(name = "priority") val priority: String = "Medium",
    @Json(name = "ai_confidence") val aiConfidence: Double? = 0.90,
    @Json(name = "ai_summary") val aiSummary: String? = null,
    @Json(name = "ai_reason") val aiReason: String? = null,
    @Json(name = "status") val status: String = "Submitted",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    // Joined relations (optional in queries)
    @Json(name = "service") val service: CivicService? = null,
    @Json(name = "department") val department: Department? = null,
    @Json(name = "category") val category: ServiceCategory? = null,
    @Json(name = "ward") val ward: Ward? = null,
    // Phase 7 Administrative fields
    @Json(name = "assigned_team_id") val assignedTeamId: String? = null,
    @Json(name = "assigned_team_name") val assignedTeamName: String? = null,
    @Json(name = "assigned_at") val assignedAt: String? = null,
    @Json(name = "assigned_by") val assignedBy: String? = null,
    @Json(name = "resolution_text") val resolutionText: String? = null,
    @Json(name = "resolved_at") val resolvedAt: String? = null,
    @Json(name = "resolved_by") val resolvedBy: String? = null,
    @Json(name = "closed_at") val closedAt: String? = null,
    @Json(name = "closed_by") val closedBy: String? = null
)

/**
 * Payload sent when inserting a new complaint into Supabase.
 * Only database-validated fields are passed. Privileged fields like status are set to 'Submitted'.
 */
@JsonClass(generateAdapter = true)
data class CreateComplaintPayload(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "department_id") val departmentId: String? = null,
    @Json(name = "description") val description: String,
    @Json(name = "original_transcript") val originalTranscript: String,
    @Json(name = "input_mode") val inputMode: String = "voice",
    @Json(name = "language") val language: String = "en-IN",
    @Json(name = "location_text") val locationText: String,
    @Json(name = "ward_id") val wardId: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "location_accuracy_meters") val locationAccuracyMeters: Double? = null,
    @Json(name = "location_source") val locationSource: String? = "manual",
    @Json(name = "priority") val priority: String = "Medium",
    @Json(name = "ai_confidence") val aiConfidence: Double? = 0.90,
    @Json(name = "ai_summary") val aiSummary: String? = null,
    @Json(name = "ai_reason") val aiReason: String? = null,
    @Json(name = "status") val status: String = "Submitted"
)
