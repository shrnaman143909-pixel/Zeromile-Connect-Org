package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Phase 7 Administrative Data Models
 */

@JsonClass(generateAdapter = true)
data class AdminUser(
    val id: String,
    val email: String,
    val role: String = "admin", // "citizen", "municipal_staff", "admin"
    val fullName: String? = null,
    val token: String? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
    val isStaffOrAdmin: Boolean get() = role.equals("admin", ignoreCase = true) || role.equals("municipal_staff", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class UserRoleRecord(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "role") val role: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Team(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "department_id") val departmentId: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminAuditLogRecord(
    @Json(name = "id") val id: String,
    @Json(name = "admin_user_id") val adminUserId: String? = null,
    @Json(name = "admin_email") val adminEmail: String? = null,
    @Json(name = "action") val action: String,
    @Json(name = "entity_type") val entityType: String = "complaint",
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "metadata") val metadata: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

enum class DateRangeFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

enum class AdminSortBy(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    RECENTLY_UPDATED("Recently Updated"),
    PRIORITY("Priority (High to Low)")
}

data class AdminComplaintFilter(
    val status: String? = null,
    val priority: String? = null,
    val departmentId: String? = null,
    val wardId: String? = null,
    val serviceId: String? = null,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL,
    val searchQuery: String = "",
    val sortBy: AdminSortBy = AdminSortBy.NEWEST,
    val page: Int = 1,
    val pageSize: Int = 10
)

data class AdminMetrics(
    val total: Int = 0,
    val submitted: Int = 0,
    val assigned: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0,
    val closed: Int = 0,
    val byCategory: Map<String, Int> = emptyMap(),
    val byWard: Map<String, Int> = emptyMap(),
    val byPriority: Map<String, Int> = emptyMap()
)

data class PagedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
) {
    val totalPages: Int get() = if (pageSize > 0) kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize).toInt()) else 1
    val hasNextPage: Boolean get() = page < totalPages
    val hasPrevPage: Boolean get() = page > 1
}

@JsonClass(generateAdapter = true)
data class UpdateComplaintAdminPayload(
    @Json(name = "status") val status: String? = null,
    @Json(name = "assigned_team_id") val assignedTeamId: String? = null,
    @Json(name = "assigned_team_name") val assignedTeamName: String? = null,
    @Json(name = "assigned_at") val assignedAt: String? = null,
    @Json(name = "assigned_by") val assignedBy: String? = null,
    @Json(name = "resolution_text") val resolutionText: String? = null,
    @Json(name = "resolved_at") val resolvedAt: String? = null,
    @Json(name = "resolved_by") val resolvedBy: String? = null,
    @Json(name = "closed_at") val closedAt: String? = null,
    @Json(name = "closed_by") val closedBy: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
