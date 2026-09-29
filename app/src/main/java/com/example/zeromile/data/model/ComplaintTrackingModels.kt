package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard municipal complaint lifecycle status enumeration.
 */
enum class ComplaintStatus(val rawValue: String, val label: String) {
    SUBMITTED("Submitted", "Submitted"),
    ASSIGNED("Assigned", "Assigned"),
    IN_PROGRESS("In Progress", "In Progress"),
    RESOLVED("Resolved", "Resolved"),
    CLOSED("Closed", "Closed");

    companion object {
        fun fromString(value: String?): ComplaintStatus {
            return entries.find { it.rawValue.equals(value, ignoreCase = true) } ?: SUBMITTED
        }
    }
}

/**
 * Historical record of a complaint status transition in the database.
 * Table: complaint_status_history
 */
@JsonClass(generateAdapter = true)
data class ComplaintStatusHistoryRecord(
    @Json(name = "id") val id: String,
    @Json(name = "complaint_id") val complaintId: String,
    @Json(name = "status") val status: String,
    @Json(name = "note") val note: String? = null,
    @Json(name = "assigned_team") val assignedTeam: String? = null,
    @Json(name = "changed_by") val changedBy: String? = null,
    @Json(name = "created_at") val createdAt: String
)

/**
 * Official municipal update issued for a complaint.
 * Table: complaint_updates
 */
@JsonClass(generateAdapter = true)
data class ComplaintUpdateRecord(
    @Json(name = "id") val id: String,
    @Json(name = "complaint_id") val complaintId: String,
    @Json(name = "message") val message: String,
    @Json(name = "official_name") val officialName: String? = "Nagpur Municipal Corporation",
    @Json(name = "created_by") val createdBy: String? = null,
    @Json(name = "created_at") val createdAt: String
)

/**
 * Composite tracking model bundling complaint record, chronological status history,
 * and official municipal updates for display on the Complaint Detail screen.
 */
data class ComplaintTrackingDetails(
    val complaint: ComplaintRecord,
    val history: List<ComplaintStatusHistoryRecord>,
    val updates: List<ComplaintUpdateRecord>,
    val evidence: List<ComplaintEvidenceRecord> = emptyList(),
    val assignedTeam: String? = null,
    val resolutionNote: String? = null,
    val resolvedAt: String? = null
)
