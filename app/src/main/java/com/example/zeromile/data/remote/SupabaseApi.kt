package com.example.zeromile.data.remote

import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.CreateComplaintPayload
import com.example.zeromile.data.model.Department
import com.example.zeromile.data.model.Profile
import com.example.zeromile.data.model.ServiceCategory
import com.example.zeromile.data.model.Ward
import com.example.zeromile.data.model.Team
import com.example.zeromile.data.model.AdminAuditLogRecord
import com.example.zeromile.data.model.SupabaseAuthTokenResponse
import com.example.zeromile.data.model.UserRoleRecord
import com.example.zeromile.data.model.UpdateComplaintAdminPayload
import com.example.zeromile.data.model.NotificationRecord
import com.example.zeromile.data.model.CreateNotificationPayload
import com.example.zeromile.data.model.UpdateNotificationReadPayload
import com.example.zeromile.data.model.ComplaintEvidenceRecord
import com.example.zeromile.data.model.CreateEvidencePayload
import com.example.zeromile.data.model.SignUrlPayload
import com.example.zeromile.data.model.SignUrlResponse
import com.example.zeromile.data.model.EmergencyService
import com.example.zeromile.data.model.CivicUpdate
import com.example.zeromile.data.model.CreateCivicUpdatePayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SupabaseApi {
    @GET("rest/v1/service_categories?select=*&order=name.asc")
    suspend fun getServiceCategories(): List<ServiceCategory>

    @GET("rest/v1/services?select=*,category:service_categories(*),department:departments(*)&is_active=eq.true&order=name.asc")
    suspend fun getServices(
        @Query("category_id") categoryFilter: String? = null
    ): List<CivicService>

    @GET("rest/v1/services?select=*,category:service_categories(*),department:departments(*)&order=name.asc")
    suspend fun getAllServicesAdmin(): List<CivicService>

    @GET("rest/v1/departments?select=*&order=name.asc")
    suspend fun getDepartments(): List<Department>

    @GET("rest/v1/wards?select=*&order=ward_number.asc")
    suspend fun getWards(): List<Ward>

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfiles(
        @Query("user_id") userIdFilter: String? = null
    ): List<Profile>

    @POST("rest/v1/complaints")
    suspend fun createComplaint(
        @Body payload: CreateComplaintPayload
    ): List<ComplaintRecord>

    @GET("rest/v1/complaints?select=*,service:services(*),department:departments(*),category:service_categories(*),ward:wards(*)&order=created_at.desc")
    suspend fun getComplaints(
        @Query("user_id") userIdFilter: String? = null
    ): List<ComplaintRecord>

    @GET("rest/v1/complaints?select=*,service:services(*),department:departments(*),category:service_categories(*),ward:wards(*)")
    suspend fun getComplaintById(
        @Query("id") idFilter: String
    ): List<ComplaintRecord>

    @GET("rest/v1/complaint_status_history?select=*&order=created_at.asc")
    suspend fun getStatusHistory(
        @Query("complaint_id") complaintIdFilter: String
    ): List<com.example.zeromile.data.model.ComplaintStatusHistoryRecord>

    @GET("rest/v1/complaint_updates?select=*&order=created_at.asc")
    suspend fun getComplaintUpdates(
        @Query("complaint_id") complaintIdFilter: String
    ): List<com.example.zeromile.data.model.ComplaintUpdateRecord>

    // Phase 7 Administrative Endpoints
    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Body payload: Map<String, String>
    ): SupabaseAuthTokenResponse

    @GET("rest/v1/user_roles?select=*")
    suspend fun getUserRoles(
        @Query("user_id") userIdFilter: String
    ): List<UserRoleRecord>

    @GET("rest/v1/teams?select=*&order=name.asc")
    suspend fun getTeams(): List<Team>

    @GET("rest/v1/complaints?select=*,service:services(*),department:departments(*),category:service_categories(*),ward:wards(*)")
    suspend fun getAdminComplaints(
        @Query("status") statusFilter: String? = null,
        @Query("priority") priorityFilter: String? = null,
        @Query("department_id") departmentFilter: String? = null,
        @Query("ward_id") wardFilter: String? = null,
        @Query("order") order: String? = "created_at.desc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<ComplaintRecord>

    @PATCH("rest/v1/complaints")
    suspend fun updateComplaintAdmin(
        @Query("id") idFilter: String,
        @Body payload: UpdateComplaintAdminPayload
    ): List<ComplaintRecord>

    @POST("rest/v1/complaint_status_history")
    suspend fun createStatusHistoryAdmin(
        @Body payload: com.example.zeromile.data.model.ComplaintStatusHistoryRecord
    ): List<com.example.zeromile.data.model.ComplaintStatusHistoryRecord>

    @POST("rest/v1/complaint_updates")
    suspend fun createComplaintUpdateAdmin(
        @Body payload: com.example.zeromile.data.model.ComplaintUpdateRecord
    ): List<com.example.zeromile.data.model.ComplaintUpdateRecord>

    @POST("rest/v1/admin_audit_logs")
    suspend fun createAuditLogAdmin(
        @Body payload: Map<String, @JvmSuppressWildcards Any?>
    ): List<AdminAuditLogRecord>

    @GET("rest/v1/admin_audit_logs?select=*&order=created_at.desc")
    suspend fun getAuditLogsAdmin(
        @Query("limit") limit: Int? = 50
    ): List<AdminAuditLogRecord>

    @DELETE("rest/v1/complaints")
    suspend fun deleteComplaintAdmin(
        @Query("id") idFilter: String
    ): Response<Unit>

    // Phase 8: Notification Endpoints
    @GET("rest/v1/notifications?select=*&order=created_at.desc")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String? = null,
        @Query("limit") limit: Int = 50
    ): List<NotificationRecord>

    @PATCH("rest/v1/notifications")
    suspend fun markNotificationRead(
        @Query("id") idFilter: String,
        @Body payload: UpdateNotificationReadPayload
    ): List<NotificationRecord>

    @POST("rest/v1/rpc/mark_all_notifications_read")
    suspend fun markAllNotificationsRead(): Response<Int>

    @POST("rest/v1/notifications")
    suspend fun createNotification(
        @Body payload: CreateNotificationPayload
    ): List<NotificationRecord>

    // Phase 9: Evidence & Supabase Storage Endpoints
    @GET("rest/v1/complaint_evidence?select=*&order=created_at.desc")
    suspend fun getEvidenceByComplaintId(
        @Query("complaint_id") complaintIdFilter: String
    ): List<ComplaintEvidenceRecord>

    @POST("rest/v1/complaint_evidence")
    suspend fun createEvidence(
        @Body payload: CreateEvidencePayload
    ): List<ComplaintEvidenceRecord>

    @DELETE("rest/v1/complaint_evidence")
    suspend fun deleteEvidence(
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/complaint_evidence")
    suspend fun deleteEvidenceByComplaintId(
        @Query("complaint_id") complaintIdFilter: String
    ): Response<Unit>

    @POST("storage/v1/object/complaint-evidence/{path}")
    suspend fun uploadStorageObject(
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("storage/v1/object/sign/complaint-evidence/{path}")
    suspend fun createSignedUrl(
        @Path(value = "path", encoded = true) path: String,
        @Body payload: SignUrlPayload
    ): SignUrlResponse

    @DELETE("storage/v1/object/complaint-evidence/{path}")
    suspend fun deleteStorageObject(
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    // Phase 10: Emergency Services Endpoints
    @GET("rest/v1/emergency_services?select=*&is_active=eq.true&order=sort_order.asc")
    suspend fun getEmergencyServices(): List<EmergencyService>

    @GET("rest/v1/emergency_services?select=*&order=sort_order.asc")
    suspend fun getAllEmergencyServicesAdmin(): List<EmergencyService>

    // Phase 10: Civic Updates Endpoints
    @GET("rest/v1/civic_updates?select=*,ward:wards(*)&is_active=eq.true&order=published_at.desc")
    suspend fun getCivicUpdates(
        @Query("ward_id") wardFilter: String? = null
    ): List<CivicUpdate>

    @GET("rest/v1/civic_updates?select=*,ward:wards(*)&order=created_at.desc")
    suspend fun getAllCivicUpdatesAdmin(): List<CivicUpdate>

    @POST("rest/v1/civic_updates")
    suspend fun createCivicUpdate(
        @Body payload: CreateCivicUpdatePayload
    ): List<CivicUpdate>

    @PATCH("rest/v1/civic_updates")
    suspend fun updateCivicUpdate(
        @Query("id") idFilter: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any?>
    ): List<CivicUpdate>

    @DELETE("rest/v1/civic_updates")
    suspend fun deleteCivicUpdate(
        @Query("id") idFilter: String
    ): Response<Unit>
}

object SupabaseClientProvider {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var authTokenProvider: (() -> String?)? = null

    fun setAuthTokenProvider(provider: (() -> String?)?) {
        authTokenProvider = provider
    }

    fun getAuthToken(): String? {
        return authTokenProvider?.invoke()
    }

    fun create(baseUrl: String, publishableKey: String): SupabaseApi {
        // Ensure baseUrl ends with '/'
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = authTokenProvider?.invoke() ?: publishableKey
            val request = original.newBuilder()
                .header("apikey", publishableKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(SupabaseApi::class.java)
    }
}
