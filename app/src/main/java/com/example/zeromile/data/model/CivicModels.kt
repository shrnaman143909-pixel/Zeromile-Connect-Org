package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Department(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Ward(
    @Json(name = "id") val id: String,
    @Json(name = "ward_number") val wardNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "city") val city: String = "Nagpur",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ServiceCategory(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CivicService(
    @Json(name = "id") val id: String,
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "department_id") val departmentId: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "service_type") val serviceType: String = "complaint",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "process_description") val processDescription: String? = null,
    @Json(name = "required_information") val requiredInformation: String? = null,
    @Json(name = "requires_location") val requiresLocation: Boolean = true,
    @Json(name = "supports_evidence") val supportsEvidence: Boolean = true,
    @Json(name = "is_complaint_service") val isComplaintService: Boolean = true,
    @Json(name = "popular_score") val popularScore: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    // Joined relations (optional)
    @Json(name = "category") val category: ServiceCategory? = null,
    @Json(name = "department") val department: Department? = null
)

@JsonClass(generateAdapter = true)
data class EmergencyService(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "alternate_phone") val alternatePhone: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CivicUpdate(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "category") val category: String? = "General",
    @Json(name = "priority") val priority: String = "normal", // 'low', 'normal', 'high', 'urgent'
    @Json(name = "ward_id") val wardId: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_by") val createdBy: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    // Joined relations (optional)
    @Json(name = "ward") val ward: Ward? = null
)

@JsonClass(generateAdapter = true)
data class CreateCivicUpdatePayload(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "priority") val priority: String = "normal",
    @Json(name = "ward_id") val wardId: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_by") val createdBy: String? = null
)

@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "city") val city: String = "Nagpur",
    @Json(name = "preferred_language") val preferredLanguage: String = "en",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val role: String = "citizen"
)
